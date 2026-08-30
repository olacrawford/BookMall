# PIKE: Stage 03 AI 决策域

## 元信息

- 阶段：Stage 03 - AI 决策域
- 分支：`stage/03-ai-decision`
- 状态：编码中

## P - Problem

这里写：AI 决策域解决什么问题，为什么不能直接把 LLM 返回文本当成业务结果。

重点：

- 模型没有业务数据
- 模型输出不稳定
- Agent 不能获得资金操作权限
- 决策必须带证据和规则版本

## I - Insight

这里写：最难的三个点。

建议围绕：

- 工具白名单和业务权限
- 结构化输出校验
- RAG 证据如何可靠落入 Decision

## K - Key Decisions

| 问题 | 候选方案 | 选择 | 原因 | 影响 | 是否需要确认 |
| --- | --- | --- | --- | --- | --- |
| LLM 供应商 | 只接真实 API / Mock 先行 | Mock + Provider 接口 | 无 Key 可跑通 | 后续替换实现 | 是 |
| RAG 存储 | 向量库 / MySQL | 先 MySQL | 降低第一周复杂度 | 后续加向量 | 是 |
| 工具暴露 | 直接 HTTP / MCP 协议 | MCP 风格契约 | 统一工具边界 | 多一层接口 | 是 |

## E - Evidence

验收后填写：

- `Decision` 样例
- 工具清单
- RAG 召回结果
- 非法 action 拒绝测试
- 面试可写点
