# CHANGELOG

## 2026-09-01

- 新增 `AfterSaleOutboxService`：退款结果与 `t_after_sale_outbox` 在同一事务写入，提供扫描投递和幂等消费。
- 新增 `OutboxEventController`：`GET /internal/outbox/scan`、`POST /internal/outbox/{eventId}/consume`。
- 新增 `RabbitMqConfig`、`OutboxDeliveryMessage`、`AfterSaleOutboxPublisher`、`AfterSaleOutboxConsumer`：`scanAndDispatch` 通过 RabbitTemplate 发布事件，成功后置 `DISPATCHED`，`@RabbitListener` 消费复用幂等入口。
- `bookmall-common` 新增售后 exchange/queue/routing key 常量，`bookmall-after-sale/pom.xml` 增加 `spring-boot-starter-amqp`。
- 新增 `WorkflowRecoveryService` 和 `WorkflowRecoveryController`：失败步骤置为 `FAILED`，恢复扫描转 `RETRYING`，完成后工作流恢复 `RUNNING`。
- 新增 `LogisticsQueryGateway`、`LogisticsQueryResult`、`DemoLogisticsQueryGateway`：物流查询可注入，`mode=ok/timeout/unavailable` 支持故障注入。
- 新增 `LogisticsRecoveryService` 和 `LogisticsRecoveryController`：物流超时降级为 `WAITING_HUMAN`，`POST /internal/logistics/{afterSaleId}/recover` 恢复处理。
- `AfterSaleStatusMachine` 增加 `WAITING_HUMAN`，允许 `UNDER_REVIEW/PROCESSING -> WAITING_HUMAN -> PROCESSING/REJECTED/CANCELED`。
- `AfterSaleServiceImpl.createAfterSale` 在 Policy 前执行物流检查，查询不可用时写失败步骤、审计并直接返回人工状态。
- 新增 `AfterSaleAccessGuard`：售后单归属校验返回 403，审批权限改为 JWT `role` 优先、配置白名单兜底。
- `ApprovalTaskController` 列表、审批、驳回接口均接收 `X-User-Roles` 并接入审批人校验。
- auth 增加 `User.role`，注册默认 `USER`，`JwtUtil` 写入 `role` claim，`LoginResponse` / `UserVO` 返回角色。
- Gateway `AuthGlobalFilter` 解析 JWT role 并透传 `X-User-Roles`；新增 `TraceIdGlobalFilter` 透传 `X-Request-Id`、`X-Trace-Id`。
- `AfterSaleApplication` 增加 `@EnableScheduling`，Outbox 扫描和工作流恢复扫描定时执行。
- `AfterSaleServiceImpl` 自动退款和人工退款均写 Outbox，售后单查询/详情/退款走归属保护。
- 新增 `sql/updates/007_after_sale_role.sql`，同步更新 `sql/sql.txt` 的 `t_user`。
- 配置新增：RabbitMQ、`after-sale.approver-ids`、`approver-roles`、`logistics.mode`、`outbox.dispatch.delay-ms`、`initial-delay-ms`、工作流恢复参数。
- 新增单测：`AfterSaleOutboxServiceTest`、`AfterSaleOutboxPublisherTest`、`AfterSaleOutboxConsumerTest`、`WorkflowRecoveryServiceTest`、`AfterSaleAccessGuardTest`、`LogisticsRecoveryServiceTest`、`DemoLogisticsQueryGatewayTest`。
- 验证结果：after-sale 51 个测试通过，auth 4 个通过，gateway 4 个通过，全项目 80 个测试通过，`git diff --check` 无错误。

## 当前限制

- 单测/编译级证据，尚未在本地 Docker RabbitMQ 上做真实断链、重连端到端演练。
- 物流 adapter 为 demo 故障注入实现，真实物流服务可通过替换 `LogisticsQueryGateway` 接入。
