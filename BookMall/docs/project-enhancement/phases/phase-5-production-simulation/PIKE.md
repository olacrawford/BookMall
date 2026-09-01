# PIKE: Stage 05 Production Simulation

## P - Problem

正常路径跑通不等于交付完成。阶段 5 要防的是“演示一次成功就写进简历”：重复请求是否产生新售后单、AI 结果是否能从落库反查、Worker 中断后是否真的能继续，任何一个没证据都有可能把模拟能力包装成生产能力。

## I - Insight

端到端演示的价值不在截图，而在可重复的输入、稳定输出和数据库终态。真正可信的闭环由三层构成：API 返回同一个业务 ID、DB 只有一次副作用、审计/Outbox/证据能指向同一链路。任何一层对不上，就说明实现或演示脚本有问题，先修再收口。

## K - Key Decisions

| 问题 | 候选 | 选择 | 原因 | 影响 |
| --- | --- | --- | --- | --- |
| 幂等键来源 | 只信任 DTO / 同时读请求头 | Controller 读 `Idempotency-Key` 并写入 DTO | 客户端常用 Header 传幂等键，符合契约和已有脚本 | 唯一键继续做并发兜底 |
| evidence 格式 | 只数组 / 只字符串 / 兼容 | 自定义反序列化器兼容数组和字符串 | 契约保持数组，旧命令不破坏 | DTO 多一个反序列化器 |
| AI 分析展示 | 页面重算 / 落库后查询 | 新增 AnalysisQueryService 查持久化记录 | 证据必需可审计、可反查 | 查询依赖 decision/evidence/tool 表 |
| 中断恢复演示 | 只测到 step RETRYING / 走完业务终态 | 注入失败后恢复并继续退款 | 证明业务能继续，不只证明状态能改 | 文档区分 checkpoint 恢复与自动完成 |
| 真实程度 | 宣称生产可用 / 标记本地模拟 | 所有 Provider 标 `simulation` | 防止把 Mock 写成生产结论 | 简历只能写接口与本地模拟 |

## E - Evidence

- EV-009：低金额自动退款 `1008`，同 key 重放仍 `1008`，退款 1 条；高金额 `1007` 审批后退款完成；普通用户审批 403。
- EV-010：step 30 `FAILED -> RETRYING -> COMPLETED`，attempt 2，售后单从 `PROCESSING` 继续退款到 `COMPLETED`。
- EV-014：`GET /after-sales/1003/analysis` 能查回 `VALID` Decision、5 个工具结果、6 个证据和 2 条规则命中；Outbox 事件 `CREATED -> DISPATCHED -> CONSUMED`。
- 售后模块单测 72 个通过，新增 DTO 兼容测试和持久化分析查询测试。
