USE bookmall;

-- 阶段 4：售后规则文档与分块种子。
-- 脚本可重复执行；先插文档，再按 document_code + policy_version 幂等插 chunk。

INSERT INTO t_rag_document (document_code, title, category, policy_version, permission_scope, status)
SELECT 'AFTER_SALE_LOGISTICS_NOT_RECEIVED', '物流签收未收到处置规则', 'LOGISTICS', 'v1', 'PUBLIC', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM t_rag_document
    WHERE document_code = 'AFTER_SALE_LOGISTICS_NOT_RECEIVED' AND policy_version = 'v1'
);

INSERT INTO t_rag_document (document_code, title, category, policy_version, permission_scope, status)
SELECT 'AFTER_SALE_DAMAGED', '商品破损处置规则', 'GOODS', 'v1', 'PUBLIC', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM t_rag_document
    WHERE document_code = 'AFTER_SALE_DAMAGED' AND policy_version = 'v1'
);

INSERT INTO t_rag_document (document_code, title, category, policy_version, permission_scope, status)
SELECT 'AFTER_SALE_MISSING_ITEM', '少件缺件处置规则', 'GOODS', 'v1', 'PUBLIC', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM t_rag_document
    WHERE document_code = 'AFTER_SALE_MISSING_ITEM' AND policy_version = 'v1'
);

INSERT INTO t_rag_document (document_code, title, category, policy_version, permission_scope, status)
SELECT 'AFTER_SALE_REFUND_COMPENSATION', '退款与补偿通用规则', 'POLICY', 'v1', 'PUBLIC', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM t_rag_document
    WHERE document_code = 'AFTER_SALE_REFUND_COMPENSATION' AND policy_version = 'v1'
);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 1,
       '用户反馈物流显示签收但未收到时，先核对签收时间、签收地点、驿站取件记录和用户自述；签收地点非本人且无有效取件记录时，不得仅凭签收状态自动退款，应先人工核实。',
       '签收,未收到,驿站,取件记录,人工核实', 80
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_LOGISTICS_NOT_RECEIVED' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 1);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 2,
       '若用户已补充门卫或驿站无包裹记录，且物流轨迹与签收地点存在明显矛盾，可按已审核证据进入退款或补偿评估；金额高于 50 元或风险等级非 LOW 时必须人工审批。',
       '无包裹,门卫,驿站,退款,人工审批', 85
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_LOGISTICS_NOT_RECEIVED' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 2);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 1,
       '商品破损需用户提供拆包照片、外包装和订单快照作为证据；证据不足时转人工核实，不得仅凭文字描述执行退款。',
       '破损,拆包照片,证据,人工核实', 70
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_DAMAGED' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 1);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 2,
       '破损严重且订单金额不高于 50 元、用户风险为 LOW 时，可在人工复核证据后退款；否则进入审批或风控。',
       '破损,退款,审批,风控', 65
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_DAMAGED' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 2);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 1,
       '少件缺件先核对订单明细、配送重量和用户提供的开箱视频；只有可确认商品未发出的证据才允许补发或退款。',
       '少件,缺件,开箱,补发,退款', 70
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_MISSING_ITEM' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 1);

INSERT INTO t_rag_chunk (document_id, chunk_no, content, keywords, token_count)
SELECT d.id, 1,
       '退款与补偿必须以订单金额为上限，原因、金额和证据必须同时存在；金额高于 50 元或风险非 LOW 时进入人工审批，AI 只给建议。',
       '退款,补偿,金额上限,人工审批,AI建议', 80
FROM t_rag_document d
WHERE d.document_code = 'AFTER_SALE_REFUND_COMPENSATION' AND d.policy_version = 'v1'
  AND NOT EXISTS (SELECT 1 FROM t_rag_chunk c WHERE c.document_id = d.id AND c.chunk_no = 1);
