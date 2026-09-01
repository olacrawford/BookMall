USE bookmall;

-- 阶段 3：售后单列表当前只按 user_id + create_time 排序。
-- 旧索引 (user_id, status, create_time) 导致 MySQL 在 EXPLAIN 中出现 Using filesort；
-- 拆出专用排序索引后，同一查询可走 idx_after_sale_user_create_time，无 filesort。

ALTER TABLE t_after_sale_order ADD INDEX idx_after_sale_user_create_time (user_id, create_time);

-- 当前代码没有按 user_id + status + create_time 的售后单查询；如后续出现该查询，用
-- (user_id, status) 或单独 status 索引替代，并重新 EXPLAIN。
ALTER TABLE t_after_sale_order DROP INDEX idx_after_sale_user_status;

-- t_approval_task 无 status 参数的队列排序在 1000 行样本中仍选择全表扫描，
-- 增加的 create_time 索引没有稳定收益，按阶段 3 停止条件不再保留。
-- t_audit_log 已有 idx_audit_trace(trace_id)，无需新增索引。
