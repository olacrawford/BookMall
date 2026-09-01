# 阶段 2：处理重复、超时和越权（第 4 天）

## 目标与业务价值

让失败、重试、并发和越权成为可复现的系统行为，而不是演示时避开的偶然情况。

这一阶段直接对应简历里的“分布式长链路最终一致性”：重点不是把某个按钮做出来，而是证明跨服务、异步消息和长流程在抖动时仍能收敛到正确状态。

## 为什么这么做

阶段 1 已经证明业务链路能跑通，但售后链路里真正的高风险点不在正常路径，而在失败路径：

- 退款已经成功，但退款事件没有被可靠送达消费方，不能导致后续重复退款或状态漂移。
- 同一事件被扫描或投递两次，不能产生两个副作用。
- Worker 中断或步骤失败后，流程要从断点恢复，而不是停在半成品状态。
- 普通用户不能读别人的售后单，也不能操作审批任务。
- 物流查询超时不能阻塞整个售后创建链路，也不应自动放行高风险退款。

这些是最终一致性的核心约束，不是“以后再加”的体验优化。

## 这样做的好处

1. 退款记录和 Outbox 事件在同一个本地事务写入，能证明“业务成功则事件一定存在”。
2. 消费端用 `event_id` 做幂等，重复投递只更新一次状态，防止重复退款。
3. Outbox Worker 用 RabbitTemplate 真实发布事件，发布成功后再置 `DISPATCHED`；发布失败时事务回滚，事件保持 `CREATED`，由定时扫描和手工扫描兜底重试。
4. 工作流恢复用显式状态迁移 `FAILED -> RETRYING -> COMPLETED`，恢复过程可审计、可重试。
5. 售后单归属和审批人权限在服务入口收口，借助网关注入的 `X-User-Id` / `X-User-Roles` 做身份边界。
6. 物流查询通过可注入的 `LogisticsQueryGateway` 抽象，超时或不可用时降级为 `WAITING_HUMAN`，不自动退款。
7. Gateway 生成并透传 `X-Request-Id` / `X-Trace-Id`，后续日志和审计可以串起完整链路。

## 已完成切片

| 切片 | 实现 | 证据 |
| --- | --- | --- |
| Outbox 同事务写入 | `AfterSaleOutboxService.recordRefundExecuted` 与退款记录在同一事务中写入 | `AfterSaleOutboxServiceTest` |
| RabbitMQ Outbox Worker | `AfterSaleOutboxService.scanAndDispatch` 通过 `RabbitTemplate` 发布，成功后再置 `DISPATCHED`；`AfterSaleOutboxConsumer` 使用 `@RabbitListener` 消费 | `AfterSaleOutboxPublisherTest`、`AfterSaleOutboxConsumerTest`、`AfterSaleOutboxServiceTest` |
| 至少一次扫描投递 | 定时 `scanAndDispatch` + `GET /internal/outbox/scan`，将 `CREATED` 事件发布到 RabbitMQ 后置为 `DISPATCHED` | 单测 + 扫描接口 |
| 幂等消费 | `consume` 对 `CONSUMED` 事件直接返回成功，只更新一次；RabbitMQ 消费者复用同一幂等入口 | `AfterSaleOutboxServiceTest` |
| 工作流失败恢复 | `FAILED -> RETRYING -> COMPLETED`，完成后工作流恢复 `RUNNING` | `WorkflowRecoveryServiceTest` |
| 物流超时降级 | `LogisticsQueryGateway` + `DemoLogisticsQueryGateway`，`mode=timeout/unavailable` 时创建售后单降级为 `WAITING_HUMAN`，物流步骤标记失败并写审计 | `DemoLogisticsQueryGatewayTest`、`LogisticsRecoveryServiceTest`、`AfterSaleStatusMachineTest` |
| 售后单归属保护 | `AfterSaleAccessGuard.requireOwner` 越权返回 403 | `AfterSaleAccessGuardTest` |
| JWT role + 审批权限 | `User`/`JwtUtil` 写入 `role`，Gateway 透传 `X-User-Roles`，`requireApprover` 角色优先、配置白名单兜底 | `AuthGlobalFilterTest`、`AfterSaleAccessGuardTest` |
| 链路追踪 | Gateway `TraceIdGlobalFilter` 生成/透传 `X-Request-Id`、`X-Trace-Id` | gateway 单测/编译通过 |

## 新增内部接口

```text
GET  /internal/outbox/scan?limit=10
POST /internal/outbox/{eventId}/consume
POST /internal/logistics/{afterSaleId}/recover
POST /internal/workflow/recover?limit=10
POST /internal/workflow/steps/{stepId}/fail?errorCode=WORKER_INTERRUPTED&errorMessage=xxx
POST /internal/workflow/steps/{stepId}/complete
```

这些接口用于故障演练和验收；生产环境应只对内部网络暴露，并通过 Worker 调度替代手工触发。

## 配置变更

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: 123456

after-sale:
  approver-ids: 1
  approver-roles: APPROVER
  logistics:
    mode: ok # ok/timeout/unavailable，用于故障注入演示
  outbox:
    dispatch:
      delay-ms: 10000
      initial-delay-ms: 5000
  workflow:
    recovery:
      delay-ms: 30000
      initial-delay-ms: 10000
```

审批权限采用“JWT role claim 优先、配置白名单兜底”：新登录用户携带 `role`，旧 JWT 或尚未登录鉴权的内部调用仍可通过 `after-sale.approver-ids` 放行指定审批人。`logistics.mode` 是 demo adapter 的故障注入开关，真实环境替换 `LogisticsQueryGateway` 实现即可。

## 验证方案

```bash
mvn -f BookMall/pom.xml -pl bookmall-after-sale,bookmall-auth,bookmall-gateway -am test
mvn -f BookMall/pom.xml -q test
git diff --check
```

已验证：

- after-sale 模块 51 个测试通过。
- auth 模块 4 个测试通过。
- gateway 模块 4 个测试通过。
- 全项目共 80 个测试通过。
- Outbox 同一事件消费两次只执行一次状态更新。
- RabbitMQ 发布和消费入口分别有单测覆盖。
- 普通用户访问他人售后单和审批接口返回 403。
- 工作流失败步骤可恢复为 `RETRYING`，完成后可恢复工作流为 `RUNNING`。
- 物流超时/不可用时售后单进入 `WAITING_HUMAN`，人工恢复后可回到 `PROCESSING`。

## 当前限制

- RabbitMQ 发布、消费已接入并有用例覆盖，但本阶段证据仍是单测和编译级；尚未在本地 Docker RabbitMQ 上做真实断链、重连和积压恢复的端到端演练。
- `DemoLogisticsQueryGateway` 是故障注入实现，不是真实物流服务；它用于证明“查询失败 -> 降级人工 -> 恢复流程”这条业务语义。
- JWT 只包含新签发 token 的角色 claim；旧 token 若没有 `role`，审批权限会回退到配置白名单，用户重新登录后即可走角色校验。

## 风险与回滚

- Outbox 定时扫描在事务内同步发布，RabbitMQ 不可用时发布异常会回滚本次扫描，事件保持 `CREATED`；可以先暂停定时调度，恢复 MQ 后再用手工扫描补发。
- 权限错误优先关闭审批和售后写接口，不让普通用户绕过业务入口。
- 工作流恢复默认 30 秒一次，参数可通过配置调整；不要设置过短导致抖动放大。
- `sql/updates/007_after_sale_role.sql` 给 `t_user` 增加 `role` 并把 ID 1 置为 `APPROVER`；生产环境执行前应核对既有管理员账号。

## 停止条件

重复消费、越权访问、工作流恢复、物流降级任一单测失败，冻结继续扩展，先修复可靠性边界。

## 面试卡片

一句话简历话术：售后长链路用同事务 Outbox + RabbitMQ Worker 做至少一次投递与幂等消费，用可注入物流 adapter 和可恢复步骤状态机收敛超时与 Worker 中断，用服务入口归属/JWT 角色校验收敛越权。

三层追问：

1. 为什么不用“业务前先发 MQ”：因为本地事务未提交时消息可能已经可见，Outbox 把可靠投递建立在业务事务上。
2. 为什么是至少一次而不是最多一次：分布式环境无法保证零丢失，真正需要的是消费端幂等。
3. 为什么审批权限放在服务入口：网关只负责认证和注入可信身份，服务层必须再次校验归属和角色。

失败场景：退款成功但事件消费失败时，事件保持 `DISPATCHED`，重扫/重投后消费端通过 `event_id` 幂等收敛，不会重复退款；物流查询持续超时时，售后单不再自动退款，而是进入 `WAITING_HUMAN` 等人为处置。

## 状态

`completed`：可靠性三项已完成，包括 JWT role 审批权限、可注入物流超时降级 adapter、RabbitMQ Outbox Worker；剩余真实 MQ 端到端故障演练属于运行验证，不阻塞阶段收口。
