# THOUGHT

阶段 3 最容易犯的错是把“加索引”直接当成“优化了”。真正有用的顺序是先建立可复现样本，再做 EXPLAIN，确认查询计划和代价变化，再决定是否保留。

本次 EXPLAIN 得到三个很清晰的结论：

1. 售后单列表用了 `(user_id, status, create_time)`，但查询只按 `user_id` 过滤、按 `create_time` 排序，所以出现 `Using filesort`。拆成 `(user_id, create_time)` 后，Extra 变成 `Backward index scan`，filesort 消失。
2. 审批队列不传 status 时，1000 行样本里 `create_time` 索引没有让 MySQL 选择索引排序。原因是样本小、全表扫描和 filesort 的代价更低；这时保留索引只会增加写放大，应该停止，不为了“看起来优化过”硬留。
3. 审计 trace 查询原本就有 `idx_audit_trace`，不需要为了凑数量重复加索引。

另一个不能外推的点是：这次只有本机 MySQL 1000 行样本、单实例 Spring Boot 和 5 并发 HTTP 基线，不是生产级高并发压力。现阶段有真实 P50/P95/P99、吞吐、错误率和资源占用，可以说明“完成了可复现基线并做了一次有依据的索引调整”，但不能写成已验证的生产 P95 或高并发优化。
