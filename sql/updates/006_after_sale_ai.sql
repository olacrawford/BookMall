USE bookmall;

-- 上游查询契约确认（bookmall-after-sale 通过 OpenFeign 调用）：
-- 1) 订单详情  GET /orders/{id} + X-User-Id
--    OrderDetailVO: id, orderNo, userId, totalAmount,
--    status(0待支付/1已支付/2已取消/3已完成), expireTime,
--    receiverName, receiverPhone, receiverAddress,
--    items[{bookId, bookTitle, bookPrice, quantity, subtotal}]
-- 2) 支付单    GET /payment/order/{orderId} + X-User-Id
--    PaymentVO: id, paymentNo, orderId, orderNo, amount,
--    payType, status(0待支付/1已支付/2失败), payTime
-- 3) 库存查询  GET /stock/{bookId}
--    StockVO: bookId, stock, lockedStock, availableStock


-- 电商售后重构的一周基线表。脚本可重复执行，不删除旧交易表。

CREATE TABLE IF NOT EXISTS t_after_sale_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    after_sale_no VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    policy_version VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    UNIQUE KEY uk_after_sale_idempotency (idempotency_key),
    KEY idx_after_sale_user_status (user_id, status, create_time),
    KEY idx_after_sale_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单';

CREATE TABLE IF NOT EXISTS t_after_sale_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    after_sale_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    description VARCHAR(2000) NOT NULL,
    decision_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    trace_id VARCHAR(128) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no),
    UNIQUE KEY uk_ticket_after_sale (after_sale_id),
    KEY idx_ticket_user_time (user_id, create_time),
    KEY idx_ticket_decision_status (decision_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后工单';

CREATE TABLE IF NOT EXISTS t_ticket_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL COMMENT 'USER/AI/OPERATOR/SYSTEM',
    sender_id BIGINT DEFAULT NULL,
    content VARCHAR(4000) NOT NULL,
    evidence_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_ticket_time (ticket_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单消息';

CREATE TABLE IF NOT EXISTS t_workflow_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_no VARCHAR(64) NOT NULL,
    ticket_id BIGINT NOT NULL,
    workflow_type VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    current_step VARCHAR(40) DEFAULT NULL,
    workflow_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    context_json JSON DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workflow_no (workflow_no),
    UNIQUE KEY uk_workflow_ticket (ticket_id),
    KEY idx_workflow_status_time (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后流程实例';

CREATE TABLE IF NOT EXISTS t_workflow_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    step_key VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME DEFAULT NULL,
    lease_until DATETIME DEFAULT NULL,
    checkpoint_json JSON DEFAULT NULL,
    last_error_code VARCHAR(64) DEFAULT NULL,
    last_error_message VARCHAR(500) DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workflow_step (workflow_id, step_key),
    KEY idx_step_retry (status, next_retry_time),
    KEY idx_step_lease (status, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程步骤与检查点';

CREATE TABLE IF NOT EXISTS t_approval_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    workflow_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    assignee_id BIGINT DEFAULT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'WAITING',
    decision VARCHAR(24) DEFAULT NULL,
    comment VARCHAR(1000) DEFAULT NULL,
    decided_at DATETIME DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_task_no (task_no),
    KEY idx_approval_status_time (status, create_time),
    KEY idx_approval_assignee (assignee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工审批任务';

CREATE TABLE IF NOT EXISTS t_refund_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL,
    after_sale_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(160) NOT NULL,
    provider_ref VARCHAR(128) DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_refund_idempotency (idempotency_key),
    KEY idx_refund_order (order_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录';

CREATE TABLE IF NOT EXISTS t_compensation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    compensation_no VARCHAR(64) NOT NULL,
    after_sale_id BIGINT NOT NULL,
    type VARCHAR(24) NOT NULL COMMENT 'COUPON/CASH/RESEND',
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(160) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_compensation_no (compensation_no),
    UNIQUE KEY uk_compensation_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补偿/重发记录';

CREATE TABLE IF NOT EXISTS t_risk_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    score DECIMAL(6,3) DEFAULT NULL,
    hit_rules_json JSON DEFAULT NULL,
    source VARCHAR(24) NOT NULL DEFAULT 'RULE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_risk_ticket (ticket_id),
    KEY idx_risk_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险判断';

CREATE TABLE IF NOT EXISTS t_policy_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_version VARCHAR(32) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    effective_at DATETIME NOT NULL,
    expire_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy_version (policy_version),
    KEY idx_policy_effective (status, effective_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后规则版本';

CREATE TABLE IF NOT EXISTS t_policy_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_version VARCHAR(32) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    condition_json JSON NOT NULL,
    action VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy_rule (policy_version, rule_code),
    KEY idx_policy_rule_match (policy_version, enabled, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后规则';

CREATE TABLE IF NOT EXISTS t_ai_decision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(32) NOT NULL,
    decision_json JSON NOT NULL,
    validation_status VARCHAR(24) NOT NULL,
    raw_output TEXT DEFAULT NULL,
    latency_ms INT DEFAULT NULL,
    input_tokens INT DEFAULT NULL,
    output_tokens INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_decision_ticket (ticket_id, create_time),
    KEY idx_ai_decision_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 决策记录';

CREATE TABLE IF NOT EXISTS t_ai_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decision_id BIGINT NOT NULL,
    evidence_id VARCHAR(160) NOT NULL,
    evidence_type VARCHAR(32) NOT NULL COMMENT 'ORDER/LOGISTICS/RISK/POLICY',
    source_ref VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    policy_version VARCHAR(32) DEFAULT NULL,
    permission_scope VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_evidence (decision_id, evidence_id),
    KEY idx_ai_evidence_type (evidence_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 证据';

CREATE TABLE IF NOT EXISTS t_tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    arguments_json JSON NOT NULL,
    success TINYINT NOT NULL DEFAULT 0,
    latency_ms INT DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tool_ticket_time (ticket_id, create_time),
    KEY idx_tool_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领域工具调用日志';

CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(128) NOT NULL,
    ticket_id BIGINT DEFAULT NULL,
    after_sale_id BIGINT DEFAULT NULL,
    operator_type VARCHAR(24) NOT NULL COMMENT 'USER/AI/OPERATOR/SYSTEM',
    operator_id BIGINT DEFAULT NULL,
    action VARCHAR(64) NOT NULL,
    before_status VARCHAR(32) DEFAULT NULL,
    after_status VARCHAR(32) DEFAULT NULL,
    detail_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_ticket_time (ticket_id, create_time),
    KEY idx_audit_trace (trace_id),
    KEY idx_audit_action_time (action, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后审计日志';

CREATE TABLE IF NOT EXISTS t_rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_code VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(64) DEFAULT NULL,
    policy_version VARCHAR(32) NOT NULL,
    permission_scope VARCHAR(64) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rag_document (document_code, policy_version),
    KEY idx_rag_document_scope (permission_scope, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后规则文档';

CREATE TABLE IF NOT EXISTS t_rag_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_no INT NOT NULL,
    content TEXT NOT NULL,
    keywords VARCHAR(500) DEFAULT NULL,
    vector_ref VARCHAR(200) DEFAULT NULL,
    token_count INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rag_chunk (document_id, chunk_no),
    FULLTEXT KEY ft_rag_chunk_content (content),
    KEY idx_rag_chunk_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后规则分块';

CREATE TABLE IF NOT EXISTS t_after_sale_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME DEFAULT NULL,
    last_error VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_outbox_event (event_id),
    KEY idx_outbox_dispatch (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后 Outbox 事件';

-- 最小规则种子，后续可由后台规则管理替换。
INSERT INTO t_policy_version (policy_version, policy_name, status, effective_at)
SELECT 'v1', '售后基础规则', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_policy_version WHERE policy_version = 'v1');

INSERT INTO t_policy_rule (policy_version, rule_code, condition_json, action, priority)
SELECT 'v1', 'LOW_AMOUNT_AUTO_REFUND', '{"maxAmount":50,"riskLevel":"LOW"}', 'AUTO_REFUND', 10
WHERE NOT EXISTS (SELECT 1 FROM t_policy_rule WHERE policy_version = 'v1' AND rule_code = 'LOW_AMOUNT_AUTO_REFUND');

INSERT INTO t_policy_rule (policy_version, rule_code, condition_json, action, priority)
SELECT 'v1', 'HIGH_AMOUNT_APPROVAL', '{"minAmount":50}', 'REQUIRE_APPROVAL', 20
WHERE NOT EXISTS (SELECT 1 FROM t_policy_rule WHERE policy_version = 'v1' AND rule_code = 'HIGH_AMOUNT_APPROVAL');
