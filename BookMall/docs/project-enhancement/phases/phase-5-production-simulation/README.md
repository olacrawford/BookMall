# 阶段 5：生产模拟与闭环证据（第 6 天下午至第 7 天）

## 状态

`completed`：两条主路径、幂等、权限、流程恢复、持久化 AI 分析、Outbox 本地链路和 Gateway/JWT API 链路均已实测留证。

## 目标与业务价值

把前面阶段的能力变成可重复演示、可审计、可面试的交付物，而不是只停留在单测。演示要证明：低风险自动退款和高风险人工审批都能走完，重复请求不会产生重复副作用，Worker 中断后能从 checkpoint 恢复，AI 分析证据可以从数据库重新查回。

## 已完成切片

| 切片 | 实现 | 证据/位置 |
| --- | --- | --- |
| 固定演示种子 | `sql/updates/010_phase5_demo_seed.sql` 提供低金额订单 10001 和高金额订单 10002 | 已执行 |
| 请求头幂等 | `AfterSaleController.create` 读取 `Idempotency-Key` 并写入请求 | `AfterSaleController.java` |
| 证据数组兼容 | `FlexibleEvidenceDeserializer` 同时接受数组和旧字符串 | `AfterSaleCreateRequestTest` |
| 持久化 AI 分析查询 | `GET /after-sales/{id}/analysis` 返回落库 Decision、工具日志、证据和规则命中 | `AfterSaleAnalysisQueryService` |
| 自动退款闭环 | 用户创建低金额售后单，自动完成流程、退款、Outbox 发布与消费 | EV-009 |
| 人工审批闭环 | 高金额进入审批，审批后恢复流程，退款后最终 `COMPLETED` | EV-009/EV-010 |
| 权限验收 | 普通用户访问审批队列返回 403，审批人可操作 | EV-009 |
| 网关 JWT 验收 | 经 8080 注册/登录后调用售后、审批、退款和 AI 分析 | EV-009 |
| 前端售后控制台 | Vue 3 页面：我的售后单、创建、详情、AI 分析、审批待办 | EV-015 |
| 流程中断恢复 | 步骤标记 `FAILED`，恢复为 `RETRYING`，完成后继续业务 | EV-010 |
| 长链路一致性 | Decision/Evidence/Tool/Outbox/Audit 可关联并在最终状态收敛 | EV-014 |

## 实测结果

### 1. 低金额自动退款 + 请求头幂等

使用 `Idempotency-Key: PH5-EVIDENCE-ARRAY-20260901-03` 两次调用 `POST /after-sales`，两次都返回同一个售后单：

```text
afterSaleId=1008
status=COMPLETED
policyAction=AUTO_REFUND
refund id=5，status=SUCCESS，amount=39.80
t_refund_record 中 after_sale_id=1008 的记录数 = 1
t_after_sale_outbox：REFUND_EXECUTED-1008-... CREATED -> DISPATCHED -> CONSUMED
```

### 2. 高金额人工审批 + 恢复 + 退款

```text
POST /after-sales（orderId=10002） -> afterSaleId=1007，status=WAITING_APPROVAL，policyAction=REQUIRE_APPROVAL
POST /approval-tasks/1004/approve -> 售后单 PROCESSING，审批任务 APPROVED
POST /internal/workflow/steps/30/fail?errorCode=WORKER_INTERRUPTED -> step 30 FAILED
POST /internal/workflow/recover?limit=10 -> data=1，step 30 RETRYING
POST /internal/workflow/steps/30/complete -> true，step 30 COMPLETED，attempt_count=2
POST /after-sales/1007/refund -> refund id=4，SUCCESS，金额 199.00
最终：售后单 COMPLETED，workflow COMPLETED，退款记录 1 条
```

审批后的流程中断恢复证明的是“可继续执行”的 checkpoint 语义；本演示随后人工触发退款完成终态，不把中断恢复包装成自动退款恢复。

### 3. 权限

```text
GET /approval-tasks?status=WAITING，X-User-Id: 7
-> {"code":403,"message":"普通用户不能操作审批任务"}
```

### 4. 持久化 AI 分析

```text
GET /after-sales/1003/analysis，X-User-Id: 7
-> validationStatus=VALID，action=NEEDS_HUMAN
-> 5 个工具结果，6 个证据 ID，2 条规则命中
-> 数据来自 t_ai_decision / t_tool_call_log / t_ai_evidence，不是页面临时重算
```

### 5. Gateway/JWT API 验收

通过 8080 网关完成注册、登录，并用同一 JWT 调用售后、审批、退款和 AI 分析：

```text
POST /api/auth/register + /api/auth/login -> ph5_user，role=APPROVER
低金额 orderId=10001 -> afterSaleId=1009，COMPLETED，退款 39.80 一条
同 Idempotency-Key 重放 -> 仍返回 1009，无重复退款
高金额 orderId=10002 -> afterSaleId=1010，WAITING_APPROVAL
审批队列定位任务 1005 -> APPROVED，售后单 PROCESSING
POST /api/after-sales/1010/refund -> refund id=7，SUCCESS，199.00
最终：1010 COMPLETED，workflow COMPLETED，Outbox CONSUMED
GET /api/after-sales/1009/analysis -> VALID，5 个工具结果，证据和规则命中
```

## 本阶段修复

1. 创建接口原先只读 DTO 内 `idempotencyKey`，`verify-week-one.sh` 通过请求头传幂等键时会生成新售后单。改为读取 `Idempotency-Key` 请求头。
2. 接口契约和验收脚本里 `evidence` 是数组，但 DTO 曾只接受字符串，导致脚本请求 500。增加兼容反序列化器，数组按契约处理，旧字符串继续可用。
3. 增加持久化 AI 分析查询，避免“分析了但详情页无法反查”的证据断层。
4. `verify-week-one.sh` 原来对低金额和高金额共用同一个 `Idempotency-Key`，会导致高金额创建命中低金额单。改为低金额与高金额使用独立幂等键，脚本可完整跑通两条路径。

## 限制与真实性声明

- LLM 是 `MockLlmClient`，物流是 `DemoLogisticsQueryGateway`，退款是 `provider_ref=MOCK`；全部属于本地 `simulation`。
- 本阶段同时完成了 8093 直连、8080 Gateway/JWT API 和 Vue 3 前端售后控制台接入；已通过 Chrome 无头浏览器完成登录、创建售后、查看详情/AI 分析和审批操作验收，并保存 3 张截图。未录制人工视频，因此浏览器证据以截图和脚本结果为准。
- RAG 是 MySQL 关键词召回，不是 ES/Milvus；没有真实 Provider、真实物流、真实资金渠道和跨实例容量结论。
- 审批队列被阶段 3 的 1000 条性能种子占据，演示通过 DB 固定 `ticket_id`/任务号定位，不把队列分页能力镀成生产后台。

## 验证命令

```bash
mvn -f BookMall/pom.xml -pl bookmall-after-sale -am test -q
mvn -f BookMall/pom.xml -q test
git diff --check
cd front && npm run build
npm run dev -- --host 0.0.0.0 --port 5173
TEST_TOKEN='Bearer <JWT>' IDEMPOTENCY_KEY='PH5-GATEWAY-FINAL-20260901-06' \
  ./BookMall/docs/project-enhancement/scripts/verify-week-one.sh
```

当前实测：售后模块 72 个测试通过，全项目 101 个测试通过，`git diff --check` 无提示；Chrome 无头浏览器验收覆盖低金额创建、详情/AI 分析、高金额创建和审批，截图见 EV-015。

## 面试证据

阶段 5 的证据文件：`EV-009.md`（两条主路径/幂等/权限）、`EV-010.md`（流程恢复）、`EV-014.md`（AI 长链路一致性）、`EV-015.md`（前端售后控制台）。
