# Stage 02 售后业务域执行手册

## 本阶段目标

把 Stage 01 的设计落地成可运行后端服务 `bookmall-after-sale`。

完成后你能：

- 启动售后服务
- 创建售后单/工单
- 做状态迁移
- 按策略判定自动/审批/风控
- 拒绝重复退款
- 写审计日志

## 前置依赖

- 完成 Stage 01 并验收
- 至少 MySQL、BookMall 基础设施可启动
- 已读 [AI学习地图](../../AI学习地图.md) 中“状态机、工作流、幂等、审计”

## 先学习的概念

| 概念 | 理解到什么程度 |
| --- | --- |
| MyBatis-Plus | 看现有 BookMall 模块如何写 Mapper |
| 状态机 | 集中定义迁移表，服务层调用统一入口 |
| 工作流 | 每个步骤落库，可查询状态和失败重试 |
| 幂等 | 同一退款请求重复执行结果一致 |
| 审计 | 关键动作记录操作者、动作、前后状态、trace_id |

## 要做的事

按顺序执行。

- [ ] 1. 在父 `pom.xml` 增加模块 `bookmall-after-sale`，端口定为 `8093`。
- [ ] 2. 参考现有 `bookmall-order` 的分层创建 `controller/dto/entity/mapper/service/service.impl/config/client/mq`。
- [ ] 3. 创建增量 SQL `sql/updates/006_after_sale_ai.sql`，至少覆盖 Stage 01 定稿的表：
      - `after_sale_order`
      - `ticket`
      - `ticket_message`
      - `workflow_instance`
      - `workflow_step`
      - `approval_task`
      - `refund_record`
      - `risk_record`
      - `policy_version`
      - `policy_rule`
      - `audit_log`
      - `outbox`
      - `rag_document`
      - `rag_chunk`
- [ ] 4. 为订单、退款等字段设计唯一业务键和索引：
      - `refund_no` 唯一
      - `ticket_id` 唯一
      - 状态和用户查询索引
- [ ] 5. 创建实体与 Mapper，不用手写复杂 SQL。
- [ ] 6. S级：实现 `AfterSaleStatusMachine`，集中管理状态迁移。
- [ ] 7. S级：实现 `PolicyService`，输入工单/金额/风险，输出：
      - 自动处理
      - 人工审批
      - 风控介入
- [ ] 8. S级：实现 `WorkflowService`：
      - 创建流程实例
      - 推进步骤
      - 步骤失败可重试
      - 状态可查询
- [ ] 9. S级：实现 `RefundService`：
      - 先检查是否已有 `refund_no`
      - 已存在直接返回，不重复退款
      - 退款记录、订单状态更新、审计必须在合理事务内完成
- [ ] 10. 实现 `AuditService`，至少记录：创建、AI 建议、规则命中、审批、退款、失败。
- [ ] 11. 实现基础 Controller：
      - `POST /after-sales`
      - `GET /after-sales/{id}`
      - `GET /after-sales`
      - `POST /approval-tasks/{id}/approve`
      - `POST /approval-tasks/{id}/reject`
- [ ] 12. 写单元测试，覆盖：
      - 正常状态迁移
      - 非法状态迁移抛异常
      - 重复退款幂等
      - 审计写入
- [ ] 13. 运行测试并记录结果。
- [ ] 14. 更新 `CHANGELOG.md` / `THOUGHT.md` / `PIKE.md`。
- [ ] 15. 更新本阶段面试沉淀。

## 输出文件

```text
bookmall-after-sale/...
sql/updates/006_after_sale_ai.sql
docs/stages/stage-02-after-sale-core/PIKE.md
docs/stages/stage-02-after-sale-core/CHANGELOG.md
docs/stages/stage-02-after-sale-core/THOUGHT.md
```

## S级手写点

- 状态迁移函数
- 策略判定逻辑
- 工作流步骤推进
- 退款幂等判断
- 审计关键字段

AI 可代写：

- 模块骨架
- Mapper/Entity/Controller 模板
- SQL 初稿
- 测试骨架

## 验收清单

- [ ] `mvn -f BookMall/pom.xml -pl bookmall-after-sale -am test` 通过
- [ ] `bookmall-after-sale` 服务能启动
- [ ] 后端能通过接口创建售后单
- [ ] `CREATED -> COMPLETED` 非法迁移被拒绝
- [ ] 重复提交同一 `refund_no` 只产生一条退款记录
- [ ] 审计日志包含创建、规则、审批或自动处理、退款动作
- [ ] PIKE 记录了 K/E
- [ ] 面试沉淀已写入

## 面试沉淀

简历候选：

```text
实现售后工单、状态机、策略引擎、人工审批、退款幂等与审计服务，非法状态迁移被拒绝，重复退款不会重复执行。
```

面试问题：

1. 状态迁移为什么放在一个类里？

   回答重点：唯一入口，避免不同 Service 各自改状态，方便校验和审计。

2. 重复退款怎么处理？

   回答重点：业务幂等键 + 数据库唯一约束 + 状态前置检查。

3. 审批通过后流程怎么继续？

   回答重点：审批任务完成任务后，调用工作流推进下一个步骤；不是另写一套退款逻辑。

主动讲失败场景：

```text
退款接口被调用两次。第一次成功写入退款，第二次通过 refund_no 查到已存在直接返回，不创建新退款。
```

## 完成后下一步

进入 [Stage 03](./../stage-03-ai-decision/README.md) 实现 AI 决策域。
