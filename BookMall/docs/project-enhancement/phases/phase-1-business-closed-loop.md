# 阶段 1：先跑通售后主流程（第 1 天下午至第 3 天）

## 目标与业务价值

完成投诉 -> 售后单 -> 规则判定 -> 自动处理/审批 -> 退款记录 -> 审计的确定性闭环，先让业务成立，再接 AI。

## 前置条件

阶段 0 通过；确认订单、支付、库存查询契约；生成增量 SQL（建议 `006_after_sale_ai.sql`）。

## 今天照着做

1. 新建 `bookmall-after-sale` 包结构：`entity/mapper/service/controller/dto/vo`，先让空服务能编译。
2. 在 `sql/updates/006_after_sale_ai.sql` 建售后单、工单、流程、审批、退款、审计和 Outbox 表。
3. 先写 `AfterSaleStatusMachineTest`，把合法/非法迁移列成参数化用例，再实现迁移函数。
4. 写 `PolicyServiceTest` 覆盖低金额自动、高金额审批、高风险风控三个分支。
5. 用固定 `orderId=10001` 调创建接口，检查售后单、流程 step、审计行；再重复调用同一退款 key。

## 修改范围

新增 `bookmall-after-sale`（或等价模块），包含 entity/mapper/service/controller、状态机、Policy、Workflow、Approval、Audit；只对 payment/order 增加最小退款/售后状态接口。

## 实施切片

1. 建表并加唯一键：售后业务号、`refund_idempotency_key`、流程实例和 step。
2. 手写状态迁移表和非法迁移异常。
3. 手写 Policy：金额阈值、风险等级、商品类目 -> `AUTO/APPROVAL/RISK`。
4. 工作流每一步落库，审批通过从 checkpoint 恢复。
5. 模拟退款 application service：先查幂等键，再条件创建，唯一键冲突返回已有结果。
6. 关键动作写 `audit_log`，保存操作者、前后状态、原因和 trace。
7. AI 可生成骨架、Mapper、DTO 和测试样板；人工重写上述 S 级逻辑。

## 交付产物

模块源码、SQL、状态机/Policy/幂等单测、API 示例、PIKE 记录。

## 验证方案

`mvn -pl bookmall-after-sale -am test`；重复退款并发 2 次；高金额审批批准/驳回；用户归属查询。

## 风险与回滚

新路由默认关闭；表可保留但不影响既有订单支付。跨服务调用先用 adapter，失败转人工。

## 停止条件

状态机、策略、退款幂等任一单测未通过，不接真实 AI。

## 面试证据

状态迁移表、唯一索引、审批恢复测试和审计样例。

## 必须沉淀

- `THOUGHT.md`：解释 AfterSaleOrder 与 Ticket 为什么分离，状态机/Policy/Workflow 的边界是什么。
- `PIKE.md`：至少记录一次“自动退款 vs 人工审批”的取舍和失败路径。
- `CHANGELOG.md`：列出表、API、状态、唯一键和回滚方式。
- 面试卡片：状态机非法跳转、规则版本绑定、退款幂等各准备原理/工程/取舍三层答案。

## 状态

`planned`
