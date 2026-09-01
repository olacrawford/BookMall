# PIKE: Stage 03 Performance

## P - Problem

性能问题如果不带可复现负载和查询计划，很容易变成“以为慢的地方”。售后列表、审批队列和审计 trace 是三个最常被反复查询的入口，需要先确认数据库是否真的在低效扫描，再决定是否优化。

## I - Insight

有证据的优化是先看 EXPLAIN，不先写缓存或改业务查询。

- 售后列表的 filesort 来自复合索引列顺序和排序列不一致，而不是没有索引。
- 审批队列在不传 status 时是否用索引排序由优化器根据样本代价决定，小数据量下全表扫描可能是正确答案。
- 审计 trace 已有索引时，再新增同名能力没有意义。

## K - Key Decisions

| 问题 | 候选 | 选择 | 原因 | 影响 |
| --- | --- | --- | --- | --- |
| 售后列表排序 | 保留 triple index / 拆排序索引 | 新增 `(user_id, create_time)`，移除非业务使用的 `(user_id, status, create_time)` | EXPLAIN 从 filesort 变为 Backward index scan | 后续 user+status 查询需单独评估索引 |
| 审批队列排序 | 加 create_time 索引 / 不加 | 不加 | 1000 行样本 EXPLAIN 仍全表扫描，无稳定收益 | 避免无依据写放大 |
| 审计 trace | 新建相同索引 / 使用已有索引 | 使用已有 `idx_audit_trace` | 已存在 | 无 |
| HTTP 基线 | 直接声称优化 / 跑脚本后写数字 | 已跑 100 次 5 并发并写入数字 | 不能把 EXPLAIN 等同于并发 P95 | 当前阶段为 completed |

## E - Evidence

- `t_after_sale_order` 用户列表：Before `idx_after_sale_user_status` + `Using filesort`；After `idx_after_sale_user_create_time` + `Backward index scan`。
- `t_approval_task` 无 status 队列：Before/After 都是 `ALL` + `Using filesort`，rows 1000，因此不保留索引。
- `t_audit_log` trace：Before/After 都走 `idx_audit_trace`，rows 1。
- `sql/updates/008_after_sale_performance_index.sql` 已可重放。
- HTTP 基线：list P50 4.46 ms / P95 15.55 ms / P99 23.29 ms、0% 错误；approval P50 119.60 ms / P95 139.11 ms / P99 144.07 ms、0% 错误。
