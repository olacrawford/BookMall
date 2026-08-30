# Stage 07 AI/RAG 增强执行手册

## 本阶段目标

把第一周的“可运行 AI 层”升级为“可说清、可评估、可替换”的 AI 能力。

完成后你能：

- 换真实 LLM Provider
- 用向量检索替代关键词召回（或保留可切换策略）
- 用 20 个种子问题评估 RAG
- 记录成本、Token、耗时

## 前置依赖

- Stage 01-06 已完成
- 已有待补充的规则文档数据
- 如果需要真实 LLM，准备好 API Key；没有 Key 也可以先保留 Mock

## 要做的事

- [ ] 1. 增加 `LlmProvider` 配置：
      - `mock`
      - `openai-compatible`
      启动参数可选，默认 `mock`，避免没有 Key 时跑不起来。
- [ ] 2. 增加 `EmbeddingService`，负责把 `rag_chunk` 内容转成向量。
- [ ] 3. 增加向量存储策略：
      - 方案 A：先继续 MySQL + 向量字段（只做实验）
      - 方案 B：接入 pgvector / Milvus / Redis Stack
      第一轮先跑通接口，再切换实现。
- [ ] 4. 把售后规则文档切分到 `rag_chunk`，并为每条 chunk 保存：
      - 文档 ID
      - 规则版本
      - 类目/标签
      - 内容
      - embedding
- [ ] 5. 改造 `RagSearchService`：
      - 支持关键词召回
      - 支持向量召回
      - 支持混合召回
      - 输出召回分数和来源
- [ ] 6. S级：设计 20 个 RAG 评测问题，覆盖：
      - 七天无理由
      - 物流未收到
      - 破损
      - 少件
      - 高金额审批
      - 恶意退款风险
- [ ] 7. 写评测脚本：
      - 每个问题提交查询
      - 判断预期规则是否被召回
      - 判断回答是否引用正确版本
      - 汇总正确率
- [ ] 8. 增加 LLM 调用日志：
      - prompt 长度
      - completion
      - token 数
      - 耗时
      - 成本估算
- [ ] 9. 输出一份 `docs/stages/stage-07-rag-eval/评测报告.md`。
- [ ] 10. 更新 CHANGELOG / THOUGHT / PIKE。

## S级手写点

- RAG 评测问题
- 召回策略选择
- 评测正确率计算
- Provider 切换后回归验证

## 验收清单

- [ ] Mock 模式下系统仍能运行
- [ ] 向量或混合召回已接入，不只是关键词方案
- [ ] 20 个评测用例已写入
- [ ] 评测报告包含召回正确率和证据可溯源结果
- [ ] 每个 LLM 请求记录 token 和耗时
- [ ] 真实 Provider 或 Mock Provider 都能产出合法 Decision
- [ ] 面试沉淀完成

## 面试沉淀

简历候选：

```text
实现对售后规则 RAG 的评估与可切换 LLM Provider，维护 20 个种子评测用例，按规则召回、证据版本和 token 成本记录结果。
```

面试问题：

1. 为什么需要 RAG 评测？

   回答重点：不能靠肉眼判断，必须量化规则命中率与证据可溯源。

2. 关键词和向量召回怎么选？

   回答重点：规则有标准术语时关键词够用；用户口语表述变化多时向量/混合更稳。

3. 真实模型输出和 Mock 不一致怎么办？

   回答重点：Controller/Service 只依赖 `LlmClient` 接口，Provider 替换后跑同一组回归测试和 RAG 评测。

主动讲失败场景：

```text
Mock 固定返回正确，真实模型却把“不退款”说成“可以退款”。评测集先发现证据版本不对，再调整 Prompt/检索策略。
```

## 完成后下一步

进入 [Stage 08](./../stage-08-deliver/README.md) 做面试交付。
