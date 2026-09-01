# 阶段 4：接入 AI，但不把权限交给 AI（第 5 天下午至第 6 天上午）

## 状态

`completed`：AI 中台最小版本已实现，售后创建主链路会调用 `analyze(ticketContext)`，决策、证据、工具日志和审计均落库；AI 只给建议，不持有退款或改状态权限。

## 目标与业务价值

让 AI 减少工单理解和证据收集的工作量，但不改变资金、权限和状态的最终控制权。模型输出的是结构化建议，业务侧只有经过 Schema + Policy + Workflow 才能执行写操作。

## 已完成切片

| 切片 | 实现 | 证据 |
| --- | --- | --- |
| Intent 识别 | `IntentAgent` 把自然语言归类到 `LOGISTICS_NOT_RECEIVED` / `DAMAGED` / `MISSING_ITEM` / `REFUND_REQUEST` / `GENERAL_INQUIRY` | `IntentAgentTest` |
| 证据编排 | `EvidenceAgent` 固定编排订单、物流、签收凭证、用户风险、售后规则五个只读工具 | `DomainToolRegistryTest`、工具日志 |
| 工具白名单 | `DomainToolRegistry` 只注册只读工具，统一做白名单、参数、trace、超时校验 | `GET /internal/ai/tools`、`DomainToolRegistryTest` |
| 规则召回 | `SqlRagRetriever` 按 `policy_version + permission_scope` 过滤并用关键词打分，返回条款、来源、版本、分数 | `SqlRagRetrieverTest`、RAG 落库 |
| 模型适配 | `LlmClient` 接口 + `MockLlmClient` 固定返回合法 Decision JSON；真实 Provider 只需实现同一接口 | `MockLlmClientTest` |
| 合规校验 | `DecisionValidator` + `ComplianceAgent`，非法 action、负金额、空证据、越权动作转人工 | `DecisionValidatorTest` |
| AI 审计 | `t_ai_decision`、`t_ai_evidence`、`t_tool_call_log`、`t_audit_log` 持久化 trace | EV-013 |
| 主链路接入 | `AfterSaleServiceImpl.createAfterSale` 在工单落库后调用 AI 分析；失败时转人工，不触发退款 | EV-013 |

## 核心边界

| AI 做什么 | AI 不做什么 |
| --- | --- |
| 听懂诉求、选工具、收集证据 | 不修改订单、不调用退款写接口 |
| 检索规则并返回条款/版本/分数 | 不把相似文本当作最终规则 |
| 输出建议、理由、证据 ID、下一步 | 不决定状态跳转和资金执行 |
| 失败时降级为人工建议 | 不在证据不足时自动放行 |

## 代码与配置范围

- `BookMall/bookmall-after-sale/src/main/java/com/bookmall/aftersale/ai/**`：Intent、Evidence、Compliance、LLM、Tool、RAG、审计。
- `AfterSaleServiceImpl`：创建售后主链路接入 AI，`runAiAnalysis` 只做分析和证据落库。
- `sql/updates/009_phase4_ai_intelligence.sql`：4 篇规则文档、6 个分块种子（可重复执行）。
- `nacos-config/after-sale.yaml`、`application.yml`：RabbitMQ、after-sale 本地配置。

## 验证方案

```bash
mvn -f BookMall/pom.xml -pl bookmall-after-sale -am test -q
```

启动 order 与 after-sale 服务后，独立调用 AI 中台：

```bash
curl -X POST http://localhost:8093/internal/ai/analyze -H 'Content-Type: application/json' \
  -d '{"ticketId":1003,"userId":7,"orderId":10001,"description":"包裹超过 3 天未揽收，请人工核实物流和签收证据"}'
```

创建售后主链路（使用唯一 `Idempotency-Key`）：

```bash
curl -X POST http://localhost:8093/after-sales \
  -H 'Content-Type: application/json' -H 'X-User-Id: 7' \
  -d '{"orderId":10001,"type":"LOGISTICS_NOT_RECEIVED","amount":39.80,"riskLevel":"HIGH","idempotencyKey":"<unique>"}'
```

## 实测验收（EV-013）

- 独立 AI 调用：trace `PH4-INTERNAL-FIX-1788259239`，`validationStatus=VALID`，5 个工具全部成功。
- 创建售后主链路：`afterSaleId=1002`，`ticketId=1002`，`status=RISK_REVIEW`，`decisionStatus=AI_REVIEWED`，`policyAction=RISK_CONTROL`。
- 落库：`t_ai_decision` id 6 为 `VALID`；`t_tool_call_log` id 16-20 共 5 条成功；`t_ai_evidence` id 28-33 共 6 条；`t_audit_log` 有 `AI_ANALYZE` 与 `USER CREATE` 两条。
- 结果未自动退款，主链路仍由确定性 Risk 门控进入人工风控。

## 风险与回滚

- 默认 `mock` Provider；关闭或异常时工单进入人工队列，确定性 Policy 仍可执行。
- 本次 RAG 是 MySQL 关键词召回，不是 ES/Milvus 生产检索；简历只能写本地规则检索替身。
- 策略证据 ID 改成 `policy:<version>#<documentCode>#<chunkNo>`，避免不同文档同块号碰撞唯一键。
- 回滚：删除 `phase-4` 目录及 `sql/updates/009` 种子，主链路不再调用 `runAiAnalysis` 即可回到人工/确定性路径。

## 面试卡

一句话：模型只负责理解和提出建议；订单事实来自工具，规则来自检索，最终动作必须经过 Schema、Policy 和 Workflow。

三层追问：

1. 模型输出非法 JSON 或越权动作时怎么办？答：`DecisionValidator` 拒绝并 `FALLBACK_HUMAN`，不执行。
2. RAG 召回错了怎么办？答：只记录证据和版本，规则不直接驱动动作，人工作最后裁决。
3. 工具超时怎么办？答：工具返回失败证据，AI 结果不通过时转入人工，不自动退款。

## 停止条件

AI 结果无法通过 Schema 校验或证据缺失时，停在人工处置；只有在有稳定 Provider、评测集和权限边界后才接入真实模型。
