USE bookmall;

-- 阶段 5：固定演示种子。
-- 低金额订单走自动退款路径；高金额订单走人工审批路径。
-- 脚本可重复执行；存在时跳过。

INSERT INTO t_order (id, order_no, user_id, total_amount, status, receiver_name, receiver_phone, receiver_address, create_time, expire_time, update_time)
SELECT 10001, 'ORDER-PH5-LOW-10001', 7, 39.80, 1, '验收用户', '13800000001', '上海市验收地址', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_order WHERE id = 10001);

INSERT INTO t_order (id, order_no, user_id, total_amount, status, receiver_name, receiver_phone, receiver_address, create_time, expire_time, update_time)
SELECT 10002, 'ORDER-PH5-HIGH-10002', 7, 199.00, 1, '验收用户', '13800000002', '上海市验收地址', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_order WHERE id = 10002);
