# CHANGELOG

## 2026-09-01

### AI 中台代码

- 新增 `BookMall/bookmall-after-sale/src/main/java/com/bookmall/aftersale/ai/`：
  - model：`TicketContext`、`AiDecision`、`DecisionValidation`、`EvidenceCollection`、`ToolInvocation`、`ToolResult`、`RuleHit`、`AiAnalysisResponse`。
  - service：`IntentAgent`、`EvidenceAgent`、`ComplianceAgent`、`DecisionValidator`、`JsonDecisionParser`、`AiAuditService`、`AfterSaleAiAnalysisService`、`LlmClient`、`MockLlmClient`。
  - tool：`DomainTool`、`DomainToolRegistry`、`QueryOrderTool`、`QueryLogisticsTool`、`QueryDeliveryProofTool`、`QueryUserRiskTool`、`QueryAfterSaleRuleTool`。
  - rag：`RagRetriever`、`SqlRagRetriever`。
  - controller：`InternalAiController`，提供 `POST /internal/ai/analyze`、`GET /internal/ai/tools`。

### 主链路接入

- `AfterSaleServiceImpl.createAfterSale` 在工单插入后调用 `runAiAnalysis(...)`，失败时降级 `WAITING_HUMAN`，不触发退款。
- 新增 `AfterSaleAiAnalysisService` 与 `AfterSaleStatusMachine` 之间的集成调用。

### 数据与配置

- `sql/updates/009_phase4_ai_intelligence.sql`：新增 4 篇规则文档、6 个规则分块种子；可重复执行。
- `nacos-config/after-sale.yaml`、`application.yml`：补充 RabbitMQ、after-sale 本地配置。
- `sql/sql.txt`：补充用户角色字段（配合阶段 1 权限）。

### 修复

- 策略证据 ID 从 `policy:v1#1` 改为 `policy:v1#<documentCode>#<chunkNo>`，避免不同文档同块号在 `t_ai_evidence.uk_ai_evidence` 上冲突。

### 测试

- 新增 `DecisionValidatorTest`、`IntentAgentTest`、`MockLlmClientTest`、`DomainToolRegistryTest`、`SqlRagRetrieverTest`、`AfterSaleAiAnalysisServiceTest`、`RuleHitTest`。
- 关键回归用例：非法动作转人工、不同文档同块号证据 ID 唯一。

### 文档

- 新增阶段 4 目录：`README.md`、`THOUGHT.md`、`PIKE.md`、`CHANGELOG.md`、`EV-013.md`。
- 同步 `00-index.md`、`真正入口.md`、`06-risk-and-verification.md`、`11-resume-first.md`、`evidence-index.md` 的阶段状态。
- 删除旧的单文件 `phase-4-ai-or-intelligence.md`，链接改为阶段 4 目录 README。
