USE bookmall;

-- 阶段 3 EXPLAIN 与压测样本：固定生成 1000 条售后单/工单/审批/审计样本。
-- 真实 HTTP 负载脚本默认只请求 20 次，不直接依赖这个造数脚本。

WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 1000
)
INSERT INTO t_after_sale_order (
    after_sale_no, order_id, order_no, user_id, type, status, amount,
    policy_version, idempotency_key, version, create_time
)
SELECT
    CONCAT('AS-PERF-', LPAD(i, 6, '0')),
    1000000 + i,
    CONCAT('ORDER-PERF-', LPAD(i, 6, '0')),
    1 + (i % 10),
    'LOGISTICS_NOT_RECEIVED',
    ELT(1 + (i % 4), 'UNDER_REVIEW', 'WAITING_APPROVAL', 'PROCESSING', 'COMPLETED'),
    100.00,
    'v1',
    CONCAT('PERF-KEY-', LPAD(i, 6, '0')),
    0,
    TIMESTAMP('2026-09-01 00:00:00') - INTERVAL i SECOND
FROM seq;

WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 1000
)
INSERT INTO t_after_sale_ticket (
    ticket_no, after_sale_id, user_id, description, decision_status, trace_id, create_time
)
SELECT
    CONCAT('TK-PERF-', LPAD(i, 6, '0')),
    400000 + i,
    1 + (i % 10),
    CONCAT('performance sample ', i),
    ELT(1 + (i % 4), 'UNDER_REVIEW', 'WAITING_APPROVAL', 'PROCESSING', 'COMPLETED'),
    CONCAT('PERF-TRACE-', LPAD(i, 6, '0')),
    TIMESTAMP('2026-09-01 00:00:00') - INTERVAL i SECOND
FROM seq;

WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 1000
)
INSERT INTO t_approval_task (
    task_no, workflow_id, ticket_id, assignee_id, status, decision, comment,
    decided_at, version, create_time
)
SELECT
    CONCAT('AP-PERF-', LPAD(i, 6, '0')),
    500000 + i,
    400000 + i,
    1,
    'WAITING',
    NULL,
    NULL,
    NULL,
    0,
    TIMESTAMP('2026-09-01 00:00:00') - INTERVAL i SECOND
FROM seq;

WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 1000
)
INSERT INTO t_audit_log (
    trace_id, ticket_id, after_sale_id, operator_type, operator_id, action,
    before_status, after_status, detail_json, create_time
)
SELECT
    CONCAT('PERF-TRACE-', LPAD(i, 6, '0')),
    400000 + i,
    400000 + i,
    'USER',
    1 + (i % 10),
    'CREATE',
    'CREATED',
    ELT(1 + (i % 4), 'UNDER_REVIEW', 'WAITING_APPROVAL', 'PROCESSING', 'COMPLETED'),
    JSON_OBJECT('sample', i),
    TIMESTAMP('2026-09-01 00:00:00') - INTERVAL i SECOND
FROM seq;
