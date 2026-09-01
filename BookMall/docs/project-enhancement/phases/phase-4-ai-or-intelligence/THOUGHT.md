# THOUGHT

阶段 4 的核心不是“让 AI 会做更多事”，而是先定义一个 AI 不能碰的边界。最容易踩的坑不是模型幻觉，而是把“建议”当成“执行权限”：如果工具注册表里放进退款或改状态的能力，AI 就有了资金和状态的暗门。所以这一阶段把 Agent 拆成 IntentAgent -> EvidenceAgent -> ComplianceAgent，让模型只产生 Decision JSON，写动作必须绕过 Policy 和 Workflow。

实际开发中遇到一个很具体的问题：不同规则文档的第 1 个 chunk 都生成了 `policy:v1#1`，插入 `t_ai_evidence` 时触发唯一键冲突，导致整段 `analyze` 在工具日志落库前失败。外层 `runAiAnalysis` 把异常捕获后把工单降级成 `WAITING_HUMAN`，从结果看像是“AI 没跑”，但根因是证据 ID 不够唯一。

修复方式不是放宽唯一约束，而是让证据标识更有信息量：改成 `policy:v1#AFTER_SALE_LOGISTICS_NOT_RECEIVED#1`。这样证据 ID 同时携带规则版本、文档编码和分块号，既能唯一落库，也方便审计时反查条款来源。

另一个必须手写判断的点是：本次只用 `MockLlmClient` 和 MySQL 关键词召回，不等于“真实大模型 + 向量检索”。简历上只能写“可切换的 LLM 接口 + 本地规则检索替身”，不能写已经跑 ES/Milvus 或接真实 Provider。
