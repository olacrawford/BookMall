# THOUGHT

阶段 5 最重要的理解是：验收不能只证明“第一次跑通了”，还要证明“第二次跑不会产生副作用”，并且证据必须能从 DB/API 反查。

实际开发中被两处小接口坑到：

1. `verify-week-one.sh` 一直用 `Idempotency-Key` 请求头，但 `AfterSaleController.create` 原来只从 DTO 读 `idempotencyKey`。结果同 key 重放会生成新售后单，第一次验收留下了 `afterSaleId=1005`，重复提交语义是假的。修复方式是在 Controller 入口把请求头补进 DTO，数据库唯一键继续作为兜底。
2. 契约和脚本里 `evidence` 是数组，但 DTO 是 `String`，数组一到 Jackson 就 500。这里没有把接口改成“只能数组”或“只能字符串”，而是加了一个兼容反序列化器：数组按契约处理，旧字符串也保留，避免改坏已有命令。

另外，我不能把“Worker 中断后 step COMPLETED”误写成“售后单自动完成”。本次恢复链路在 `APPROVAL_APPROVED` 步骤注入 `FAILED -> RETRYING -> COMPLETED` 后，售后单仍处于 `PROCESSING`，这是 checkpoint 恢复语义；随后由演示脚本触发退款才进入 `COMPLETED`。这种边界写进文档，才能在面试时讲清楚“恢复了什么、还剩什么”。

限制也很明确：Mock LLM、Mock 物流、Mock 退款、单实例、本地 MySQL 都不是生产结论。真实 Provider、ES/Milvus、真实资金渠道和跨实例高并发仍是后续任务。
