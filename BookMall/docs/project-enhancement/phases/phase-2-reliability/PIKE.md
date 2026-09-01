# PIKE: Stage 02 Reliability

## P - Problem

售后长链路不缺少功能，缺少的是失败时的确定性。退款成功但事件通知失败、同一事件重复投递、Worker 中断、物流查询超时、普通用户越权，这些都会让最终结果从“偶发错误”变成“重复退款或数据漂移”。

## I - Insight

可靠性的核心不是保证消息一定只投递一次，而是允许消息重复投递，同时把消费端做成幂等。

- 业务事务内写 Outbox，保证“业务成功则事件一定存在”。
- RabbitMQ 发布失败时事务回滚，事件保留 `CREATED`，扫描调度负责补发。
- 消费端按 `event_id` 判断是否已消费，重复事件不产生副作用。
- 工作流用显式状态记录恢复过程，中断不再是无迹可查的坏状态。
- 物流查询是外部依赖，超时必须降级为人工处置，不能自动放行退款。

## K - Key Decisions

| 问题 | 候选方案 | 选择 | 原因 | 影响 | 是否需要确认 |
| --- | --- | --- | --- | --- | --- |
| 投递方式 | 数据库 Outbox + 扫描 / Outbox + RabbitMQ + 扫描兜底 | Outbox + RabbitMQ，扫描定时和手工兜底 | 真实消息中间件验证传输，发布异常不丢事件 | 端到端运行验证依赖本地 RabbitMQ | 否 |
| 消费重复 | 业务再执行一次 / 幂等检查后直接成功 | 幂等检查后直接成功 | 防止重复退款 | 事件状态必须可靠更新 | 否 |
| 工作流失败处理 | 直接改回原状态 / 失败步进入可重试状态 | `FAILED -> RETRYING -> COMPLETED` | 保留重试次数和错误原因 | 恢复逻辑多一个状态 | 否 |
| 审批权限 | 仅配置白名单 / 仅 JWT role / 双层校验 | JWT role claim 优先 + 配置白名单兜底 | 新登录用户有权威角色，旧 token 不炸 | 旧 token 无角色时仍走白名单 | 否 |
| 物流超时 | 直接失败 / 自动退款 / 降级人工 | `LogisticsQueryGateway` 抽象，超时进入 `WAITING_HUMAN` | 外部依赖失败不阻塞闭环，也不越权自动退款 | 需要 demo mode 与实际 adapter 替换 | 否 |
| 链路追踪 | 各服务各自生成 ID / Gateway 透传生成 | Gateway 统一生成透传 | 日志和审计可串联 | 下游需遵守请求头 | 否 |

## E - Evidence

- `AfterSaleOutboxServiceTest`：同一事件消费两次只更新一次。
- `AfterSaleOutboxPublisherTest` / `AfterSaleOutboxConsumerTest`：RabbitTemplate 发布和 `@RabbitListener` 消费入口可独立验证。
- `AfterSaleAccessGuardTest`：他人售后单 403，普通用户审批 403，角色 header 可放行审批。
- `DemoLogisticsQueryGatewayTest` / `LogisticsRecoveryServiceTest`：超时降级 `WAITING_HUMAN`，人工恢复回 `PROCESSING`。
- after-sale 模块 51 个测试通过，auth 模块 4 个通过，gateway 模块 4 个通过，全项目 80 个测试通过。
- 新增 Outbox/RabbitMQ、Logistics 恢复、Workflow 恢复内部接口和 Gateway Trace/Role 透传。
