USE bookmall;

-- 阶段 3 EXPLAIN 基线；每行输出中重点看 type/key/rows/Extra。
-- 售后单列表：优化后应走 idx_after_sale_user_create_time，Extra 不含 Using filesort。
EXPLAIN
SELECT id, after_sale_no, order_id, order_no, status, amount, create_time
FROM t_after_sale_order
WHERE user_id = 1
ORDER BY create_time DESC;

-- 审批队列不传 status：1000 行样本仍可能选择全表扫描，阶段 3 未保留新增索引。
EXPLAIN
SELECT id, task_no, workflow_id, ticket_id, assignee_id, status, create_time
FROM t_approval_task
ORDER BY create_time ASC;

-- 审计 trace 查询：应走已有 idx_audit_trace。
EXPLAIN
SELECT id, trace_id, ticket_id, after_sale_id, action, create_time
FROM t_audit_log
WHERE trace_id = 'PERF-TRACE-0042';
