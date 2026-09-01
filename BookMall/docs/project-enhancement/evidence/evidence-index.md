# 证据索引

本表是交付门禁。状态只允许 `observed / implemented / verified / planned / simulation`。

| ID | 亮点/结论 | 证据类型 | 状态 | 计划来源 |
|---|---|---|---|---|
| EV-001 | 现有订单-库存-支付链路和基线测试 | 命令输出、代码路径 | `observed`/待实测 | 阶段 0 |
| EV-002 | 状态机拒绝非法迁移 | 单元测试 | `planned` | 阶段 1 |
| EV-003 | Policy 按金额/风险路由审批 | 单元测试 + DB | `planned` | 阶段 1 |
| EV-004 | 重复退款只有一个副作用 | 并发测试、唯一索引 | `planned` | 阶段 1/2 |
| EV-005 | Outbox 发送失败可重试 | 故障演练日志 | `planned` | 阶段 2 |
| EV-006 | 工具白名单不含写操作 | 接口响应/测试 | `verified` | 阶段 4 |
| EV-007 | RAG 返回规则条款和版本 | fixture、查询结果 | `verified`/`simulation` | 阶段 4 |
| EV-008 | 普通用户无法审批或读他人工单 | 安全测试 | `verified` | 阶段 2/5 |
| EV-009 | 自动退款/人工审批端到端闭环 | curl、DB 行、audit | `verified`/`simulation` | 阶段 5 |
| EV-010 | 流程中断后可恢复 | Worker 日志、状态快照 | `verified`/`simulation` | 阶段 2/5 |
| EV-011 | AI 中台与售后服务分层、双实例故障隔离 | 两实例日志、路由结果、接口契约 | `planned` | 阶段 4/5、后续加餐 2 |
| EV-012 | 关键词/语义混合检索和规则证据 | 评测集、召回分数、版本/权限过滤 | `planned`/`simulation` | 阶段 4、后续加餐 6 |
| EV-013 | 主控 Agent + 白名单工具受控编排 | Agent trace、工具清单、失败重试 | `verified` | 阶段 4 |
| EV-014 | AI 异步长链路最终一致性 | 故障注入、Outbox、恢复时间 | `verified`/`simulation` | 阶段 2/5 |
| EV-015 | 前端售后控制台接入 | 构建输出、Vite 代理、页面路由与 API 结果 | `verified`/`simulation` | 阶段 5 |

## 证据命名

`evidence/EV-XXX-<topic>.md`，同一证据同时保存：环境、前置数据、命令/操作、预期、实际、限制、关联代码路径。截图只作为辅助，不能替代状态和数据库结果。

## AI 特有证据

额外记录样本集版本、prompt 版本、模型/provider、工具调用、召回 chunk、权限过滤、Token/耗时/成本。Mock 结果必须标 `simulation`。
