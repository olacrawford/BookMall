# PIKE: Stage 04 AI, But Not Authority

## P - Problem

把 AI 接进售后链路时，最大的风险不是“模型答错”，而是把模型直接变成执行者。只要有写工具或自由状态跳转，模型一次越权输出就可能产生退款或错误状态。同时还需要为“AI 是否真的分析过、依据是什么、谁决定放行”留下可审计证据。

## I - Insight

AI 在本项目里是有边界的调度器，不是万能执行者。事实来自只读工具，规则来自 RAG，建议来自 LLM 输出；三者都只是决策材料，不放行任何写操作。为了让证据可反查，每次分析必须把 Decision、工具调用、证据和审计绑定到同一个 `trace_id`。

## K - Key Decisions

| 问题 | 候选 | 选择 | 原因 | 影响 |
| --- | --- | --- | --- | --- |
| AI 如何接入售后 | 直接改订单 / 只输出建议 | 只输出建议，主链路单独调 `analyze` | 权限和状态仍由 Policy/Workflow 控制 | 后续换 Provider 不影响业务 |
| 模型能力 | 接真实模型 / Mock Provider | 默认 Mock + `LlmClient` 接口 | 环境稳定、可测试、失败可回滚 | 真实 Provider 未验证 |
| 工具边界 | 全量工具 / 只读白名单 | 只注册 5 个只读工具 | 消除资金和状态暗门 | 工具扩展需评审 |
| RAG 实现 | ES/Milvus / MySQL 关键词 | 本地 MySQL 关键词召回 | 基础设施稳定、可复现 | 只能写本地替身 |
| 证据标识 | 仅 chunkNo / 文档+版本+块号 | `policy:v1#<doc>#<chunk>` | 不同文档同块号唯一，审计可反查 | 改变证据字符串格式 |

## E - Evidence

- 单测：`DecisionValidatorTest`、`IntentAgentTest`、`MockLlmClientTest`、`DomainToolRegistryTest`、`SqlRagRetrieverTest`、`AfterSaleAiAnalysisServiceTest`、`RuleHitTest` 全部通过。
- 独立 AI 调用：`validationStatus=VALID`，5 个工具日志成功。
- 创建售后主链路：`decisionStatus=AI_REVIEWED`，`status=RISK_REVIEW`，无自动退款。
- 落库：`t_ai_decision` VALID、`t_tool_call_log` 5 条、`t_ai_evidence` 6 条、`t_audit_log` 的 `AI_ANALYZE` 与 `USER CREATE`。
- 修复：策略证据 ID 由 `policy:v1#1` 改为带文档编码的全局唯一形式。
