# 差距分析与切片优先级

优先级：P0 必须完成闭环，P1 一周内增强可靠性和证据，P2 仅做设计或最小接口，P3 延后。

| Gap | Observed evidence | Business impact | Priority | Proposed slice | Dependencies | Verification |
|---|---|---|---|---|---|---|
| 没有售后业务对象 | 现有表集中在订单/支付/库存 | 无法承载投诉、退款原因和结果 | P0 | 增加售后单、工单、消息、退款记录 | SQL、after-sale service | 创建/查询用例 + DB 记录 |
| 状态可恢复能力缺失 | 订单有定时取消，未见售后流程持久化 | 中途宕机后无法知道下一步 | P0 | WorkflowInstance/Step + checkpoint | 售后状态机 | 强停后扫描恢复 |
| AI 无业务边界 | 现有没有 AI/MCP 模块 | 容易变成不可控聊天 Demo | P0 | TicketContext、Decision Schema、工具白名单 | 售后上下文 | 非法 action 被拒绝 |
| 规则未版本化 | 现有无政策表 | 规则更新后历史结果不可解释 | P0 | PolicyVersion/Rule，工单绑定版本 | 工单创建 | 同单固定版本回归 |
| 无审批门 | 现有支付直接模拟成功 | 高额赔付没有人类兜底 | P0 | Policy 按金额/风险路由审批 | 风险记录、角色 | 高额单必须等待审批 |
| 退款无幂等契约 | 支付按订单查已有支付，退款尚不存在 | 重试可能重复资金动作 | P0 | refund idempotency key + unique | 退款表 | 并发/重复调用只一条 |
| 跨服务事件非统一 | 已有支付成功 RabbitMQ | 售后结果回写容易丢失或重复 | P1 | Outbox + event_id + 幂等消费者 | RabbitMQ | 重复消息与发送失败演练 |
| 证据不可追溯 | 现有日志未统一 trace_id | 运营无法解释 AI 建议 | P1 | evidence、audit、trace 字段 | AI/MCP/售后 | 详情页和审计查询串联 |
| 物流数据缺失 | 现有代码未见物流服务 | 无法演示签收异常判断 | P1 | 本地 Mock Logistics Adapter | 工具契约 | 固定订单返回轨迹证据 |
| RAG 缺少评测 | 未发现规则文档表和评测集 | “召回正确”无法证明 | P1 | MySQL 分块 + 20 条评测用例设计 | 规则种子 | 记录命中条款/版本 |
| 运营角色缺失 | auth 只有用户身份 | 审批接口可能越权 | P1 | 最小角色字段/白名单账号 | 网关与服务校验 | 普通用户 403 |
| 观测能力不足 | 未见统一指标/Trace | 失败定位和成本核算困难 | P2 | 结构化日志、基础计数器、AI 调用日志 | 所有新模块 | curl + 日志字段检查 |
| 真实模型/向量库 | 当前无 Provider/向量依赖 | 一周引入会增加环境风险 | P3 | 保留接口，默认 Mock；后续替换 | 阶段 4/5 | Provider contract test |

## 放大风险

AI 引入后会放大三类原问题：跨服务调用超时、错误数据被模型放大、自动执行绕过权限。设计上用超时/降级、证据和 Policy 门挡住；任何无法验证的能力降为 `planned`。
