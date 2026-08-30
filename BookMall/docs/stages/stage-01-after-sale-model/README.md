# Stage 01 售后领域建模执行手册

## 本阶段目标

完成售后域的“业务理解和设计”，不写业务代码。完成后你脑中必须有：

- 售后单和工单分别是什么
- 状态机从哪到哪
- 规则版本如何绑定工单
- AI 输入什么、输出什么
- 第一周表结构

## 前置依赖

- 已读 [改造后 README](../../README-改造后.md)
- 已读 [AI学习地图](../../AI学习地图.md) 中“状态机、规则版本、Human In The Loop”
- 已创建 `docs/stages/stage-01-after-sale-model/`

## 先学习的概念

| 概念 | 理解到什么程度 |
| --- | --- |
| 状态机 | 状态只能按合法路径走，不能随便改 |
| 规则版本 | 工单创建时绑定当时规则 |
| Human In The Loop | 高金额/高风险必须人工确认 |
| 领域实体 | 先识别业务对象，再设计表字段 |

## 要做的任务

按顺序执行，每完成一项打勾。

- [ ] 1. 复制 [PIKE模板](../../PIKE模板.md) 到本目录 `PIKE.md`，先填写 P（问题）和 I（思考）。
- [ ] 2. 写出 6 个售后场景，把它当成系统需求：
      - 物流显示签收但用户没收到
      - 已付款想退款
      - 收到商品破损
      - 七天无理由退货
      - 少件/漏发
      - 疑似恶意重复退款
- [ ] 3. 对每个场景写出：用户输入 -> AI 需要知道什么 -> 系统需要执行什么。
- [ ] 4. 独立设计表清单和关键字段。至少覆盖：
      - `after_sale_order`：售后业务结果
      - `ticket`：用户沟通/客服工单
      - `workflow_instance` / `workflow_step`：流程
      - `approval_task`：审批
      - `refund_record`：退款
      - `policy_version` / `policy_rule`：规则版本
      - `audit_log`：审计
      - `outbox`：事件发送
      对每个字段写出类型、可空性、用途、索引理由。
- [ ] 5. 设计售后状态机。至少包含：
      - `CREATED`
      - `UNDER_REVIEW`
      - `APPROVAL`
      - `PROCESSING`
      - `COMPLETED`
      - `REJECTED`
      - `CANCELED`
      - `AUTO_HANDLED`
      画成表格：当前状态 + 事件 -> 下一状态。
- [ ] 6. 设计规则版本：`policy_version` 如何生效，工单创建如何绑定版本。
- [ ] 7. 设计风险等级：低中高。写明哪种需要自动、哪种需要人工、哪种进风控。
- [ ] 8. 设计 AI 契约：
      - `TicketContext`：给 AI 的输入
      - `Decision`：AI 的输出
      - `Evidence`：证据
      - `ToolResult`：工具结果
- [ ] 9. 打通一条最小链路：提交售后 -> AI 决策 -> 规则 -> 审批/自动 -> 退款 -> 审计。
- [ ] 10. 让 AI 对本阶段设计做 Review，你补充没考虑到的失败场景。
- [ ] 11. 完成 `PIKE.md` 的 E 部分，写产出和取舍。
- [ ] 12. 创建 `CHANGELOG.md` 和 `THOUGHT.md`，记录思考过程。

## 输出文件

```text
docs/stages/stage-01-after-sale-model/
  README.md
  PIKE.md
  CHANGELOG.md
  THOUGHT.md
```

## S级手写点

以下内容禁止让 AI 直接替你完成：

- 表字段设计
- 状态迁移矩阵
- 规则版本绑定方式
- 风险分级阈值
- AI Decision JSON 结构

AI 可以做：

- 把 Markdown 表格整理得更清晰
- 问你可能漏掉的边界场景
- 给你 Review 并评分

## 验收清单

- [ ] 6 个场景都写出了系统处理路径
- [ ] 每张表的关键字段都写明了类型、空性、用途、索引理由
- [ ] 状态机表格覆盖正常、驳回、取消、自动处理
- [ ] 不存在缺失的合法状态迁移
- [ ] 存在非法迁移的例子，例如直接 `CREATED -> COMPLETED` 被拒绝
- [ ] 工单创建能确定 `policy_version`
- [ ] Decision 有 `action / amount / reason / evidenceIds / policyVersion`
- [ ] 能口头讲出最小链路
- [ ] PIKE 已填写 P/I/K/E 四个部分

## 面试沉淀

简历候选：

```text
设计企业级 AI 售后运营平台的领域模型与售后状态机，核心实体覆盖工单、规则版本、审批、流程、退款和审计。
```

面试问题：

1. 为什么状态机比多个布尔字段好？

   回答重点：布尔字段无法表达“当前哪一步、下一步允许什么”，状态机限制非法跳转并方便审计。

2. 为什么规则要版本化？

   回答重点：规则会变更，历史工单要按创建时规则处理，否则结果不稳定且无法解释。

3. 一个工单能不能直接 `CREATED -> COMPLETED`？

   回答重点：不能在状态机中合法迁移，必须经过业务审核和流程步骤。

主动讲失败场景：

```text
用户反复创建退款；设计时必须用幂等键，而不是每来一次请求就创建一个售后单。
```

## 完成后下一步

进入 [Stage 02](./../stage-02-after-sale-core/README.md) 开始建表和编码。
