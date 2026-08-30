# PIKE: Stage 07 AI/RAG 增强

## 元信息

- 阶段：Stage 07 - AI/RAG 增强
- 分支：`stage/07-rag-eval`
- 状态：编码中

## P - Problem

这里写：为什么关键词 RAG 不够，为什么要评估，为什么 Provider 不能绑死。

## I - Insight

这里写：真实 LLM、向量召回、评测中最难的取舍。

## K - Key Decisions

| 问题 | 候选方案 | 选择 | 原因 | 影响 | 是否需要确认 |
| --- | --- | --- | --- | --- | --- |
| 向量库 | pgvector / Milvus / Redis Stack | 视环境选择 | 减少部署成本 | 切换实现 | 是 |
| 评估方式 | 人工看 / 评测集 | 评测集 | 可量化 | 需要维护用例 | 是 |
| Provider | Mock / 单一真实 | 接口 + Mock | 无 Key 可演示 | 配置变化 | 是 |

## E - Evidence

验收后填写：

- `评测报告.md`
- 召回正确率
- token 成本样例
- Provider 切换结果
- 面试可写点
