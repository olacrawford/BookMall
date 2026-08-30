# 一周开发接口契约

这份文件是编码时的唯一接口依据。先按契约写 DTO 和测试，再写 Controller；返回体沿用项目已有 `Result<T>`：`code`、`message`、`data`。

## 通用约定

| 项目 | 约定 |
|---|---|
| 网关地址 | `http://localhost:8080` |
| 用户身份 | Gateway 从 JWT 解析并注入 `X-User-Id`；下游禁止信任客户端同名 Header |
| 链路标识 | 客户端可传 `X-Request-Id`，Gateway 统一生成/透传 `trace_id` |
| 金额 | JSON number 输入，服务端转 `BigDecimal`；不得使用浮点做计算 |
| 时间 | ISO-8601，例如 `2026-08-30T10:00:00` |
| 幂等 | 创建售后使用 `Idempotency-Key`；退款使用服务端生成的业务幂等键 |
| 错误 | `401` 未登录，`403` 越权，`404` 不存在，`409` 状态/幂等冲突，`422` Decision 不合法，`503` 依赖不可用 |

## 用户端接口

### 创建售后工单

`POST /api/after-sales`

请求头：`Authorization: Bearer <token>`、`Idempotency-Key: <client-key>`。

```json
{
  "orderId": 10001,
  "type": "LOGISTICS_NOT_RECEIVED",
  "description": "物流显示签收，但我没有收到",
  "evidence": ["门卫处没有包裹"],
  "requestedAction": "REFUND"
}
```

返回 `data`：

```json
{
  "afterSaleId": 90001,
  "ticketId": 91001,
  "status": "UNDER_REVIEW",
  "workflowId": 92001,
  "decisionStatus": "PENDING",
  "traceId": "tr-20260830-001"
}
```

同一个用户、订单、售后类型和 `Idempotency-Key` 重复提交，返回第一次的 ID，不能创建第二条业务记录。

### 查询我的售后单

`GET /api/after-sales?status=UNDER_REVIEW&page=1&size=20`

服务端用 `X-User-Id` 过滤，忽略 query 中的 `userId`。返回 `data` 为列表和总数：

```json
{"items":[{"id":90001,"orderId":10001,"type":"LOGISTICS_NOT_RECEIVED","status":"WAITING_APPROVAL","amount":399.00}],"total":1}
```

### 查询售后详情

`GET /api/after-sales/{afterSaleId}`

返回必须包含：售后状态、订单摘要、当前流程节点、Decision、证据、审批任务（若有）、退款结果和 `traceId`。资源不属于当前用户时统一返回 `403` 或伪装为 `404`，项目内选一种并在测试固定。

### 追加用户消息

`POST /api/after-sales/{afterSaleId}/messages`

```json
{"content":"驿站确认没有包裹","evidence":["驿站查询记录"]}
```

只有售后单归属用户且未进入终态时允许追加；追加后不自动重复退款，只触发一次可幂等的重新分析任务。

## 运营和审批接口

以下接口要求 `role in {CUSTOMER_SERVICE, APPROVER, RISK}`，普通用户必须 `403`。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/after-sales/queue?status=&riskLevel=` | 客服/审批/风控 | 分页查询工单，不接受客户端 userId 过滤绕过权限 |
| GET | `/api/after-sales/{id}/analysis` | 客服/审批/风控或归属用户 | 查看 Decision、工具证据、规则版本和失败原因 |
| GET | `/api/approval-tasks?status=WAITING` | 审批人 | 只返回当前审批人可处理任务 |
| POST | `/api/approval-tasks/{id}/approve` | 审批人 | 条件更新 `WAITING -> APPROVED`，触发 Workflow 恢复 |
| POST | `/api/approval-tasks/{id}/reject` | 审批人 | 条件更新 `WAITING -> REJECTED`，流程进入终态 |
| GET | `/api/audits?ticketId=&action=&from=&to=` | 客服/风控 | 按资源和时间范围查询审计 |

审批请求：

```json
{"comment":"已核对物流签收凭证，同意按 v1 规则退款"}
```

重复审批返回已有结果；两个审批人并发时只允许一个条件更新成功，另一个得到 `409`。

## AI 内部接口

### 分析工单

`POST /internal/ai/analyze`

调用方：售后服务；必须携带内部服务凭证、`trace_id` 和 `ticketContext`。第一周可由同一进程调用，但接口不能依赖售后数据库实体。

```json
{
  "ticketId": 91001,
  "userId": 7,
  "orderId": 10001,
  "description": "物流显示签收但我没收到",
  "orderSnapshot": {"amount": 399.00,"status":"PAID"},
  "policyVersion": "v1"
}
```

返回必须符合 [schemas/decision.schema.json](./schemas/decision.schema.json)：

```json
{
  "intent":"LOGISTICS_NOT_RECEIVED",
  "action":"NEEDS_HUMAN",
  "amount":399.00,
  "reason":"先核实驿站签收证据",
  "riskLevel":"LOW",
  "evidenceIds":["order:10001","logistics:10001","policy:v1#3.2"],
  "policyVersion":"v1",
  "nextStep":"WAITING_USER"
}
```

AI 接口只返回建议，不能在本接口内创建退款、修改订单或扣库存。

## 工具接口

工具调用请求和返回分别遵循 `schemas/tool-invocation.schema.json`、`schemas/tool-result.schema.json`。第一周允许的工具：`query_order`、`query_logistics`、`query_delivery_proof`、`query_user_risk`、`query_after_sale_rule`。不存在退款、改订单、改库存工具。

## 最小错误示例

```json
{"code":422,"message":"Decision action is not allowed: REFUND_DIRECT","data":{"ticketId":91001,"fallback":"HUMAN_REVIEW"}}
```

错误响应也必须带 `traceId`（放在 `data` 或响应头，项目内统一一种格式），便于从接口追到审计和日志。
