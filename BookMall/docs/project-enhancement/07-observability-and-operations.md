# 可观测性与日常运维

第一周以本地可复现为目标，不把日志文件冒充生产监控。后续接入 Prometheus/OpenTelemetry 时沿用字段。

## 统一上下文

所有请求和事件携带：`trace_id`、`request_id`、`user_id`（服务端解析）、`ticket_id`、`after_sale_id`、`workflow_id`、`step_id`、`event_id`、`policy_version`。日志禁止记录完整手机号、地址、Token 和模型敏感输入。

## 结构化日志事件

| 事件 | 必填字段 | 级别 |
|---|---|---|
| `ticket.created` | ticket、user、order、trace | INFO |
| `ai.decision` | model/provider、prompt_version、action、risk、evidence_ids、latency_ms | INFO |
| `tool.invoked` | tool_name、allowlisted、timeout_ms、success、latency_ms | INFO/WARN |
| `policy.evaluated` | policy_version、amount、risk、route | INFO |
| `approval.decided` | task、reviewer、decision、reason | INFO |
| `refund.executed` | idempotency_key、refund_status、amount | INFO |
| `workflow.retried` | step、retry_count、next_retry_time、error_code | WARN |
| `outbox.failed` | event_id、retry_count、error_code | ERROR |

## 指标和告警草案

计数器：`after_sale_created_total`、`auto_handled_total`、`approval_waiting_total`、`refund_idempotent_hit_total`、`tool_timeout_total`、`workflow_retry_total`、`outbox_failed_total`。  
告警：待审批超过阈值、Outbox 失败重试超过上限、退款 `PENDING` 超时、工具超时率升高、非法 Decision 非零。

## 发布、回滚和数据安全

1. 发布前执行 `mvn -f BookMall/pom.xml -q test` 和 SQL dry-run；新路由默认关闭。
2. 先发布表和读路径，再打开写入口；配置默认 `llm.provider=mock`。
3. 失败时关闭售后写路由，保留查询和审计；Worker 停止重试前导出 Outbox。
4. 本地使用 Docker named volumes；演练 `mysqldump`、恢复到临时库并核对售后/审计行数。
5. 真实环境不得提交仓库中的示例密码和真实 API Key；通过 Nacos/环境变量注入。

## 容量假设

一周只验证 100 个种子工单和并发 5。扩容前必须重新测量 MySQL 索引、队列积压、工具延迟和模型成本；不从本地数据外推“10 万工单并发”结论。
