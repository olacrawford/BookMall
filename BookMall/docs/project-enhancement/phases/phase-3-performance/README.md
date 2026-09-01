# 阶段 3：测一次再决定要不要优化（第 5 天上午）

## 目标与业务价值

用小规模、可复现的测量确认瓶颈和成本，避免用平均耗时或想象中的并发包装项目。

本阶段至少完成一次并发实验（建议并发 5/10 两档），输出 P50/P95/P99、吞吐、错误率和资源占用；没有这些字段，不得在简历中写“高并发优化”。

## 已完成切片

| 切片 | 实现 | 证据 |
| --- | --- | --- |
| 可复现负载脚本 | `after-sale-load-test.sh` 支持 list/approval/create 三种模式，默认 20 次请求并输出 P50/P95/P99、吞吐、错误率 | `scripts/performance/after-sale-load-test.sh` |
| HTTP 基线 | 100 次请求、5 并发，list/approval 均无错误；P50/P95/P99、吞吐、CPU/RSS 已记录 | `EV-003.md` |
| 样本造数 | `after-sale-performance-seed.sql` 固定生成 1000 条售后单/工单/审批/审计样本 | `scripts/performance/after-sale-performance-seed.sql` |
| EXPLAIN 核对 | 售后单列表发现 `Using filesort`，审批队列无 status 是全表扫描，审计 trace 已走索引 | `EV-003.md` |
| 唯一索引优化 | 新增 `idx_after_sale_user_create_time(user_id, create_time)`，并移除当前未使用的 `(user_id, status, create_time)` 索引 | `sql/updates/008_after_sale_performance_index.sql` |
| 停止条件 | 审批队列新增 `create_time` 索引后 EXPLAIN 仍选择全表扫描，未保留该索引 | `EV-003.md` |

## 修改范围

压测脚本、SQL 索引核对、Redis 幂等/短缓存、批量查询和 RabbitMQ 消费并发参数；不改变业务语义。

本次只改了增量 SQL 和性能交付物，没有改售后状态机、接口语义或 MQ 行为。

## 验证方案

```bash
mysql -h127.0.0.1 -uroot -p123456 bookmall < sql/updates/006_after_sale_ai.sql
mysql -h127.0.0.1 -uroot -p123456 bookmall < BookMall/docs/project-enhancement/scripts/performance/after-sale-performance-seed.sql
mysql -h127.0.0.1 -uroot -p123456 bookmall < sql/updates/008_after_sale_performance_index.sql
mysql -h127.0.0.1 -uroot -p123456 bookmall < BookMall/docs/project-enhancement/scripts/performance/explain-after-sale.sql
```

启动 `bookmall-after-sale` 服务后执行 HTTP 并发基线（第 5 个参数是并发数）：

```bash
BookMall/docs/project-enhancement/scripts/performance/after-sale-load-test.sh http://localhost:8093 1 100 list 5
BookMall/docs/project-enhancement/scripts/performance/after-sale-load-test.sh http://localhost:8093 1 100 approval 5
```

实测基线（本机 MySQL 8.4、Spring Boot 单实例、5 并发、100 次请求）：

| 接口 | P50 | P95 | P99 | 平均 | 吞吐 | 错误率 |
| --- | --- | --- | --- | --- | --- | --- |
| GET /after-sales | 4.46 ms | 15.55 ms | 23.29 ms | 6.07 ms | 241.55 rps | 0% |
| GET /approval-tasks | 119.60 ms | 139.11 ms | 144.07 ms | 123.03 ms | 36.18 rps | 0% |

资源占用（`ps` 采样）：`list` 平均 CPU 7.12%、峰值 19.4%、RSS 约 253 MB；`approval` 平均 CPU 168.43%、峰值 233.3%、RSS 约 246 MB。

## 当前状态

`completed`：EXPLAIN 证据、索引调整、HTTP 并发基线、错误率和资源占用均已落盘。

## 风险与回滚

- 008 移除了 `idx_after_sale_user_status`，当前代码没有该查询；若后续出现按用户+状态的售后单列表，应恢复 `(user_id, status)` 索引并重新 EXPLAIN。
- 回滚 008：先恢复 `idx_after_sale_user_status`，再删除 `idx_after_sale_user_create_time`。
- 审批队列索引没有收益已放弃，避免无依据增加写放大。

## 停止条件

无法稳定复现实验或性能收益低于测量噪声时停止优化，保留基线。当前已触发一次：审批队列 create_time 索引没有让 EXPLAIN 从全表扫描变为索引扫描，因此未保留。
