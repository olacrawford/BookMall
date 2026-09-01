#!/usr/bin/env bash
# 阶段 3 售后接口负载脚本。
# 用法:
#   MODE=list      ./after-sale-load-test.sh http://localhost:8093 1 20
#   MODE=approval  ./after-sale-load-test.sh http://localhost:8093 1 20 approval
#   MODE=create    ./after-sale-load-test.sh http://localhost:8093 1 20 create
#   MODE=list 20 次 5 并发: ./after-sale-load-test.sh http://localhost:8093 1 20 list 5
# 输出: target/phase3/${MODE}.json
set -euo pipefail

BASE_URL="${1:-http://localhost:8093}"
USER_ID="${2:-1}"
REQUESTS="${3:-20}"
MODE="${4:-list}"
CONCURRENCY="${5:-${CONCURRENCY:-1}}"
OUT_DIR="${6:-target/phase3}"
TOKEN="${TOKEN:-}"

mkdir -p "$OUT_DIR"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

run_one() {
  local i="$1"
  local headers=( -sS -o "$WORK_DIR/body-$i" -w '%{http_code} %{time_total}' )
  if [ -n "$TOKEN" ]; then
    headers+=( -H "Authorization: Bearer $TOKEN" )
  fi

  case "$MODE" in
    list)
      curl "${headers[@]}" \
        -H "X-User-Id: $USER_ID" \
        "$BASE_URL/after-sales"
      ;;
    approval)
      curl "${headers[@]}" \
        -H "X-User-Id: $USER_ID" \
        -H "X-User-Roles: APPROVER" \
        "$BASE_URL/approval-tasks"
      ;;
    create)
      curl "${headers[@]}" \
        -H "X-User-Id: $USER_ID" \
        -H "Content-Type: application/json" \
        -d "{\"orderId\":${ORDER_ID:-10001},\"type\":\"LOGISTICS_NOT_RECEIVED\",\"description\":\"performance sample $i\",\"evidence\":[\"sample\"],\"requestedAction\":\"REFUND\"}" \
        "$BASE_URL/after-sales"
      ;;
    *)
      echo "unknown mode: $MODE" >&2
      exit 2
      ;;
  esac
  printf '\n'
}


export -f run_one
export BASE_URL USER_ID MODE TOKEN WORK_DIR

: > "$WORK_DIR/times.txt"
START_EPOCH_MS="$(python3 -c 'import time; print(int(time.time() * 1000))')"
if [ "$CONCURRENCY" -le 1 ]; then
  for i in $(seq 1 "$REQUESTS"); do
    result="$(run_one "$i")"
    printf '%s\n' "$result" >> "$WORK_DIR/times.txt"
  done
else
  seq 1 "$REQUESTS" | xargs -P "$CONCURRENCY" -I{} bash -c 'result="$(run_one {})"; printf "%s\n" "$result" >> "$WORK_DIR/times.txt"'
fi
END_EPOCH_MS="$(python3 -c 'import time; print(int(time.time() * 1000))')"

python3 - "$WORK_DIR/times.txt" "$OUT_DIR/$MODE.json" "$MODE" "$USER_ID" "$REQUESTS" "$CONCURRENCY" "$START_EPOCH_MS" "$END_EPOCH_MS" <<'PY'
import json
import statistics
import sys

times_path, out_path, mode, user_id, requests, concurrency, start_ms, end_ms = sys.argv[1:9]
pairs = []
errors = 0
with open(times_path, encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        code, elapsed = line.rsplit(' ', 1)
        pairs.append((code, float(elapsed)))
        if code != '200':
            errors += 1

durations = sorted(elapsed for _, elapsed in pairs)
n = len(durations)
def pct(p):
    if not durations:
        return 0.0
    idx = max(0, min(n - 1, int(p * n / 100)))
    return durations[idx]

first = durations[0] if durations else 0.0
last = durations[-1] if durations else 0.0
wall_seconds = max((int(end_ms) - int(start_ms)) / 1000.0, 1e-9)
summary = {
    'mode': mode,
    'userId': int(user_id),
    'requests': int(requests),
    'concurrency': int(concurrency),
    'success': n - errors,
    'errors': errors,
    'errorRate': round(errors / n, 4) if n else 0.0,
    'p50_ms': round(pct(50) * 1000, 2),
    'p95_ms': round(pct(95) * 1000, 2),
    'p99_ms': round(pct(99) * 1000, 2),
    'mean_ms': round(statistics.mean(durations) * 1000, 2) if durations else 0.0,
    'throughput_rps': round(n / wall_seconds, 2),
    'durations_ms': [round(v * 1000, 2) for v in durations],
}
with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(summary, f, ensure_ascii=False, indent=2)
print(json.dumps(summary, ensure_ascii=False, indent=2))
PY
