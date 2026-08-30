#!/usr/bin/env bash
set -euo pipefail

# 一周验收脚本：先跑无鉴权健康检查，再在提供 TEST_TOKEN 后跑业务路径。
# 用法：TEST_TOKEN='Bearer ...' ./verify-week-one.sh

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TEST_TOKEN="${TEST_TOKEN:-}"
IDEMPOTENCY_KEY="${IDEMPOTENCY_KEY:-verify-week-one-low-001}"
ORDER_ID="${ORDER_ID:-10001}"
HIGH_VALUE_ORDER_ID="${HIGH_VALUE_ORDER_ID:-10002}"
WORK_DIR="${WORK_DIR:-$(mktemp -d "${TMPDIR:-/tmp}/after-sale-verify.XXXXXX")}"

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "缺少命令: $1" >&2; exit 2; }
}

need curl
need jq

request() {
  local name="$1" method="$2" url="$3" body="${4:-}"
  local out="${WORK_DIR}/${name}.json"
  local args=(-sS -o "$out" -w '%{http_code}' -H 'Content-Type: application/json')
  [ -n "$TEST_TOKEN" ] && args+=(-H "Authorization: ${TEST_TOKEN}")
  args+=(-H "Idempotency-Key: ${IDEMPOTENCY_KEY}")
  [ -n "$body" ] && args+=(-X "$method" -d "$body") || args+=(-X "$method")
  local status
  status="$(curl "${args[@]}" "${GATEWAY_URL}${url}")"
  echo "${name}: HTTP ${status}"
  if [ "$status" -ge 500 ]; then
    cat "$out" >&2
    return 1
  fi
  jq -e . "$out" >/dev/null
  echo "${out}"
}

echo "== 1. 网关健康 =="
request health GET /api/after-sales/health >/dev/null

if [ -z "$TEST_TOKEN" ]; then
  echo "未设置 TEST_TOKEN，已完成健康检查；设置 TEST_TOKEN 后继续业务验收。"
  exit 0
fi

echo "== 2. 低金额自动处理路径 =="
low_body="$(jq -nc --argjson order "$ORDER_ID" '{orderId:$order,type:"LOGISTICS_NOT_RECEIVED",description:"物流显示签收但我没有收到",evidence:["驿站无包裹"],requestedAction:"REFUND"}')"
request low-create POST /api/after-sales "$low_body" >/dev/null
low_id="$(jq -r '.data.afterSaleId // empty' "${WORK_DIR}/low-create.json")"
[ -n "$low_id" ] || { echo '低金额创建未返回 afterSaleId' >&2; exit 1; }
request low-detail GET "/api/after-sales/${low_id}" >/dev/null

echo "== 3. 重复提交/幂等 =="
request duplicate POST /api/after-sales "$low_body" >/dev/null
duplicate_id="$(jq -r '.data.afterSaleId // empty' "${WORK_DIR}/duplicate.json")"
[ "$duplicate_id" = "$low_id" ] || { echo "幂等失败: ${low_id} != ${duplicate_id}" >&2; exit 1; }

echo "== 4. 高金额人工审批路径 =="
high_body="$(jq -nc --argjson order "$HIGH_VALUE_ORDER_ID" '{orderId:$order,type:"LOGISTICS_NOT_RECEIVED",description:"高金额订单物流异常",evidence:["签收凭证待核实"],requestedAction:"REFUND"}')"
request high-create POST /api/after-sales "$high_body" >/dev/null
high_id="$(jq -r '.data.afterSaleId // empty' "${WORK_DIR}/high-create.json")"
[ -n "$high_id" ] || { echo '高金额创建未返回 afterSaleId' >&2; exit 1; }
request high-detail GET "/api/after-sales/${high_id}" >/dev/null
jq -e '.data.status == "WAITING_APPROVAL" or .data.status == "UNDER_REVIEW"' "${WORK_DIR}/high-detail.json" >/dev/null
request approval-queue GET '/api/approval-tasks?status=WAITING' >/dev/null
task_id="$(jq -r --argjson id "$high_id" 'first(.data.items[]? | select(.afterSaleId == $id) | .id) // empty' "${WORK_DIR}/approval-queue.json")"
if [ -n "$task_id" ] && [ "$task_id" != "null" ]; then
  request approval-approve POST "/api/approval-tasks/${task_id}/approve" '{"comment":"验收脚本批准"}' >/dev/null
fi

echo "== 5. AI 分析契约 =="
request ai-analysis GET "/api/after-sales/${low_id}/analysis" >/dev/null
jq -e '.data.decision.action and (.data.decision.evidenceIds | length > 0) and .data.decision.policyVersion' "${WORK_DIR}/ai-analysis.json" >/dev/null

echo "验收请求完成。响应文件保存在: ${WORK_DIR}"
