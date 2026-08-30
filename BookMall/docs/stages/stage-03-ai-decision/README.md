# Stage 03 AI 决策域执行手册

## 本阶段目标

实现 AI 分析能力：

- 用户诉求进入 `TicketContext`
- Agent 调用受控工具收集证据
- RAG 召回售后规则
- LLM/Mock 输出结构化 `Decision`
- 决策和证据落库

完成后 AI 层不再是“聊天”，而是可以被售后业务域消费的决策服务。

## 前置依赖

- 完成 Stage 02
- 已读 [AI学习地图](../../AI学习地图.md) 中“Tool Calling、Agent、MCP、RAG”
- 已理解 Stage 01 定稿的 `TicketContext / Decision / Evidence / ToolResult`

## 先学习的概念

| 概念 | 理解到什么程度 |
| --- | --- |
| Tool Calling | 模型提出工具调用，系统执行并返回结果 |
| Agent 循环 | 模型思考 -> 调用工具 -> 拿到结果 -> 再判断 |
| MCP | 标准化的受控工具接入协议，重点是业务边界 |
| RAG | 先查规则，再让模型基于规则回答 |

## 要做的事

按顺序执行。

- [ ] 1. 创建 `bookmall-ai` 模块，端口 `8094`。
- [ ] 2. 创建 `bookmall-mcp` 模块，端口 `8095`；如果时间紧，先用 `bookmall-ai` 内的 `ToolRegistry` 模拟，后续拆独立进程。
- [ ] 3. 定义契约类：
      - `TicketContext`
      - `ToolResult`
      - `Evidence`
      - `Decision`
      - `MCPToolRequest`
      - `MCPToolResponse`
- [ ] 4. S级：定义 `Decision` 字段并校验：
      - `action`：`AUTO_REFUND` / `NEED_APPROVAL` / `REJECT` / `NEED_INFO`
      - `amount`
      - `reason`
      - `evidenceIds`
      - `policyVersion`
- [ ] 5. 实现 `LlmClient` 接口：
      - `MockLlmClient` 无 Key 可运行
      - `OpenAiCompatibleClient` 后续可替换
- [ ] 6. S级：实现 `AgentOrchestrator` 只做一件事：
      - 输入 `TicketContext`
      - 按预定义策略调用工具
      - 汇总证据和规则
      - 调用 `LlmClient` 产出 `Decision`
      - 解析失败或校验失败时返回业务错误，不让非法决策进入售后域
- [ ] 7. 实现 `ToolRegistry`，只注册查询类工具：
      - `query_order`
      - `query_logistics`
      - `query_after_sale_rule`
      - `query_user_risk_history`
      禁止注册退款、改状态、改库存等写工具。
- [ ] 8. 实现 `RagSearchService`：
      - 先管理 `rag_document` 和 `rag_chunk`
      - 根据关键词/类目/标签召回
      - 返回 `chunkId / documentId / policyVersion / content`
- [ ] 9. 写规则种子数据，例如：
      - 七天无理由规则
      - 物流未收到规则
      - 商品破损规则
      - 高价值商品人工审批
- [ ] 10. 实现 Controller：
      - `POST /ai/analyze`
      - `GET /mcp/tools`
      - `POST /mcp/tool/invoke`
- [ ] 11. 写单元测试：
      - Mock LLM 输出正常 Decision
      - 非法 action 被拒绝
      - 工具调用失败不影响工单主流程
      - RAG 能召回指定规则
- [ ] 12. 运行测试并记录。
- [ ] 13. 更新 `CHANGELOG.md` / `THOUGHT.md` / `PIKE.md`。
- [ ] 14. 更新面试沉淀。

## 输出文件

```text
bookmall-ai/...
bookmall-mcp/...
sql/updates/006_after_sale_ai.sql 中 rag_document/rag_chunk 数据
docs/stages/stage-03-ai-decision/PIKE.md
docs/stages/stage-03-ai-decision/CHANGELOG.md
docs/stages/stage-03-ai-decision/THOUGHT.md
```

## S级手写点

- `Decision` Schema 和校验
- Agent 工具调用流程
- 工具结果到证据的转换
- RAG 召回结果如何拼进 Prompt
- 非法决策拦截

AI 可代写：

- 模块骨架
- MCP 接口模板
- Mock 规则数据
- 前端 AI 分析视图初稿

## 验收清单

- [ ] `mvn -f BookMall/pom.xml -pl bookmall-ai -am test` 通过
- [ ] `POST /api/ai/analyze` 对种子工单返回结构化 `Decision`
- [ ] 输出包含 `evidenceIds` 和 `policyVersion`
- [ ] `GET /api/mcp/tools` 不包含退款/改状态工具
- [ ] 传入非法 action 时返回错误，不产生售后操作
- [ ] 工具失败时能记录错误并返回可解释结果
- [ ] RAG 对“物流显示签收但没收到”能召回物流规则
- [ ] 完成 PIKE 和面试沉淀

## 面试沉淀

简历候选：

```text
实现 AI 决策域：Agent 通过白名单 MCP 工具查询订单、物流、风控和售后规则，RAG 召回规则证据后输出结构化决策，非业务工具无法被 Agent 调用。
```

面试问题：

1. Agent 为什么需要工具？

   回答重点：模型没有订单/物流/规则数据，必须通过查询工具获得证据，再形成决策。

2. 为什么工具列表不包含退款？

   回答重点：AI 只建议，不执行；退款有金额和权限，必须由售后域策略和工作流控制。

3. 模型返回非法 JSON 怎么办？

   回答重点：校验 `Decision`，不合格直接失败重试或转人工，不能自动进入执行。

主动讲失败场景：

```text
模型把“退款”读成“REJECT”。系统通过枚举校验拒绝，而不是相信字符串。
```

## 完成后下一步

进入 [Stage 04](./../stage-04-end-to-end/README.md) 打通前端演示闭环。
