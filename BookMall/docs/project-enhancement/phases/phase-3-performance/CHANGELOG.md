# CHANGELOG

## 2026-09-01

- 新增阶段 3 压测脚本：`BookMall/docs/project-enhancement/scripts/performance/after-sale-load-test.sh`。
  - 支持 `list`、`approval`、`create` 三种模式。
  - 第 5 个参数支持并发数，输出包含 `concurrency` 和 `errorRate`。
  - 默认 20 次请求，输出 P50/P95/P99、mean、吞吐、错误率、每次耗时。
- 新增样本造数：`after-sale-performance-seed.sql`，固定生成 1000 条售后单、工单、审批任务、审计日志。
- 新增 `explain-after-sale.sql`：售后单列表、审批队列、审计 trace 三条查询的 EXPLAIN 模板。
- 新增 `sql/updates/008_after_sale_performance_index.sql`。
  - 售后单列表新增 `idx_after_sale_user_create_time(user_id, create_time)`。
  - 移除当前未使用且造成 filesort 的 `idx_after_sale_user_status(user_id, status, create_time)`。
  - 审批队列 create_time 索引因 EXPLAIN 无稳定收益，未保留。
- 新增阶段 3 文档：`README.md`、`THOUGHT.md`、`PIKE.md`、`CHANGELOG.md`、`EV-003.md`。
- 验证结果：
  - 售后单列表 EXPLAIN：`Using filesort` -> `Backward index scan`。
  - 审批队列：仍为全表扫描，触发停止条件，不优化。
  - 审计 trace：已有索引，无变化。
  - HTTP 基线已执行：list P50 4.46 ms / P95 15.55 ms / P99 23.29 ms、吞吐 241.55 rps、0% 错误；approval P50 119.60 ms / P95 139.11 ms / P99 144.07 ms、吞吐 36.18 rps、0% 错误。
