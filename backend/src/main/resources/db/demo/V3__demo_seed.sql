-- ============================================================
-- V3 演示数据（仅 dev 使用，通过 classpath:db/demo 加载）
-- 密码统一 123456，password_hash 先写占位 {demo}，
-- 由 dev 环境的 DataInitializer 启动时用 BCrypt 重新编码。
-- ============================================================

INSERT INTO student_roster (student_no, name, college, major, grade) VALUES
  ('20230001', '林小满', '计算机学院', '软件工程', '2023'),
  ('20220018', '陈屿', '机械工程学院', '机械设计制造及其自动化', '2022'),
  ('20220106', '苏梨', '外国语学院', '英语', '2022'),
  ('20230311', '周野', '体育学院', '运动训练', '2023'),
  ('20210422', '顾言', '艺术学院', '音乐表演', '2021'),
  ('20240007', '沈知夏', '经济管理学院', '工商管理', '2024');

INSERT INTO app_user (user_id, username, password_hash, nickname, student_no, role, verify_status,
                      credit_score, banned, contact, balance) VALUES
  (1,  'student',  '{demo}', '林小满',   '20230001', 'student',  'verified',   102, 0, '微信 linxm',    360.00),
  (2,  'seller',   '{demo}', '陈屿',     '20220018', 'student',  'verified',    98, 0, 'QQ 100200300',   56.50),
  (3,  'suli',     '{demo}', '苏梨',     '20220106', 'student',  'verified',   105, 0, '微信 suli_',    120.00),
  (4,  'zhouye',   '{demo}', '周野',     '20230311', 'student',  'verified',    95, 0, NULL,             32.00),
  (5,  'guyan',    '{demo}', '顾言',     '20210422', 'student',  'verified',   100, 0, NULL,             80.00),
  (10, 'operator', '{demo}', '运营小周', NULL,       'operator', 'verified',   100, 0, NULL,              0.00),
  (99, 'admin',    '{demo}', '校方管理员', NULL,     'admin',    'verified',   100, 0, NULL,              0.00);

INSERT INTO verification_record (user_id, method, submitted_student_no, submitted_name,
                                 submitted_college, submitted_major, submitted_grade,
                                 result, applied_at, verified_at) VALUES
  (1, 'manual', '20230001', '林小满', '计算机学院', '软件工程', '2023', 'approved', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY),
  (2, 'manual', '20220018', '陈屿', '机械工程学院', '机械设计制造及其自动化', '2022', 'approved', NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY),
  (3, 'manual', '20220106', '苏梨', '外国语学院', '英语', '2022', 'approved', NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
  (4, 'manual', '20230311', '周野', '体育学院', '运动训练', '2023', 'approved', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
  (5, 'manual', '20210422', '顾言', '艺术学院', '音乐表演', '2021', 'approved', NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY);

-- 平台托管账户：已支付未放款 22.00，历史流入 67.00（45+22），历史流出 45.00
UPDATE platform_account
   SET escrow_balance = 22.00, total_inflow = 67.00, total_outflow = 45.00
 WHERE account_no = 1;

INSERT INTO item (item_id, title, category_id, current_holder_id, price, original_price, purchase_price,
                  condition_code, description, status, source, notes_tag, listed_at) VALUES
  (1,  '《高等数学》第七版 上册',        12, 2,  18.00,  39.80, 39.80, 'good',     '只用了半学期，笔记很少，无缺页。', 'on_sale',  'platform', 'no_notes',   NOW() - INTERVAL 6 DAY),
  (2,  'iPhone 12 128G 蓝色',            21, 2, 899.00, 4599.00, 4299.00, 'good',  '电池 84%，无拆修，配原装充电线。', 'on_sale', 'platform', 'no_notes',  NOW() - INTERVAL 5 DAY),
  (3,  '雅马哈 F310 民谣吉他',            51, 3, 260.00,  860.00, 780.00, 'like_new', '大一买的，弹了几个月，音准很好。', 'on_sale', 'platform', 'no_notes', NOW() - INTERVAL 4 DAY),
  (4,  '护眼台灯 三档调光',               31, 3,  25.00,   69.00, 59.00, 'good',    '宿舍用了一学期，功能正常。', 'on_sale', 'offline', 'no_notes',  NOW() - INTERVAL 4 DAY),
  (5,  'Keychron K8 机械键盘',            22, 1,  45.00,  398.00, 356.00, 'good',   '红轴，已成交一次，键帽无油。', 'completed', 'platform', 'no_notes', NOW() - INTERVAL 20 DAY),
  (6,  'JBL Go3 蓝牙音箱',                23, 3,  22.00,  199.00, 169.00, 'like_new', '几乎全新，包装还在。', 'locked', 'platform', 'no_notes', NOW() - INTERVAL 3 DAY),
  (7,  '斯伯丁 7 号篮球',                 41, 4,  30.00,  129.00, 109.00, 'fair',    '室外场用过一个夏天，气足。', 'on_sale', 'platform', 'no_notes',  NOW() - INTERVAL 7 DAY),
  (8,  '捷安特 ATX 山地车',               42, 4, 150.00,  980.00, 899.00, 'good',    '毕业出，刹车和变速都正常。', 'on_sale', 'platform', 'no_notes',  NOW() - INTERVAL 8 DAY),
  (9,  '尤克里里 23 寸 单板',              52, 5,  88.00,  320.00, 289.00, 'good',    '带琴包和拨片，音色清亮。', 'on_sale', 'platform', 'no_notes',  NOW() - INTERVAL 9 DAY),
  (10, '补水面膜 10 片装',                61, 5,  15.00,   49.00, 45.00, 'new',     '未拆封，保质期到 2027。', 'pending', 'platform', 'no_notes',  NULL),
  (11, '《活着》余华 精装',               13, 2,  12.00,   45.00, 39.00, 'like_new', '翻过一遍，书脊完好。', 'on_sale', 'platform', 'no_notes',  NOW() - INTERVAL 2 DAY),
  (12, '桌面收纳箱 三层',                 32, 3,   9.90,   39.90, 35.00, 'good',    '搬宿舍出，无破损。', 'on_sale', 'offline', 'no_notes',  NOW() - INTERVAL 1 DAY);

INSERT INTO item_image (item_id, path, sort_order, is_cover, status) VALUES
  (1,  'seed:1',  0, 1, 'active'),
  (1,  'seed:11', 1, 0, 'active'),
  (2,  'seed:2',  0, 1, 'active'),
  (3,  'seed:3',  0, 1, 'active'),
  (4,  'seed:4',  0, 1, 'active'),
  (5,  'seed:5',  0, 1, 'active'),
  (6,  'seed:6',  0, 1, 'active'),
  (7,  'seed:7',  0, 1, 'active'),
  (8,  'seed:8',  0, 1, 'active'),
  (9,  'seed:9',  0, 1, 'active'),
  (10, 'seed:10', 0, 1, 'active'),
  (11, 'seed:11', 0, 1, 'active'),
  (12, 'seed:12', 0, 1, 'active');

INSERT INTO trade_order (order_no, order_type, buyer_id, seller_id, item_id, amount, delivery_method,
                         pickup_point_id, trade_status, after_sale_status, pay_deadline, confirm_deadline,
                         paid_at, completed_at) VALUES
  ('SO20251112001', 'normal', 1, 2, 5, 45.00, 'meetup', NULL, 'completed', 'none',
   NOW() - INTERVAL 3 DAY + INTERVAL 30 SECOND, NULL,
   NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 9 MINUTE),
  ('SO20251121002', 'normal', 3, 2, 6, 22.00, 'pickup', 1, 'pending_confirm', 'none',
   NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 9 MINUTE,
   NOW() - INTERVAL 59 MINUTE, NULL);

INSERT INTO item_history (item_id, event_type, event_time, description, related_user_id, order_no) VALUES
  (1,  'created', NOW() - INTERVAL 6 DAY, '建档上架：《高等数学》第七版 上册', 2, NULL),
  (2,  'created', NOW() - INTERVAL 5 DAY, '建档上架：iPhone 12 128G 蓝色', 2, NULL),
  (3,  'created', NOW() - INTERVAL 4 DAY, '建档上架：雅马哈 F310 民谣吉他', 3, NULL),
  (4,  'created', NOW() - INTERVAL 4 DAY, '建档上架：护眼台灯 三档调光', 3, NULL),
  (5,  'created', NOW() - INTERVAL 20 DAY, '建档上架：Keychron K8 机械键盘', 2, NULL),
  (5,  'deal',    NOW() - INTERVAL 3 DAY,  '成交：买家 林小满 已确认收货', 1, 'SO20251112001'),
  (6,  'created', NOW() - INTERVAL 3 DAY,  '建档上架：JBL Go3 蓝牙音箱', 3, NULL),
  (7,  'created', NOW() - INTERVAL 7 DAY,  '建档上架：斯伯丁 7 号篮球', 4, NULL),
  (8,  'created', NOW() - INTERVAL 8 DAY,  '建档上架：捷安特 ATX 山地车', 4, NULL),
  (9,  'created', NOW() - INTERVAL 9 DAY,  '建档上架：尤克里里 23 寸 单板', 5, NULL),
  (11, 'created', NOW() - INTERVAL 2 DAY,  '建档上架：《活着》余华 精装', 2, NULL),
  (12, 'created', NOW() - INTERVAL 1 DAY,  '建档上架：桌面收纳箱 三层', 3, NULL);

INSERT INTO delivery_record (order_no, method, agreed_time, meetup_place, pickup_code,
                             verify_time, inbound_time, pickup_point_id) VALUES
  ('SO20251112001', 'meetup', NOW() - INTERVAL 3 DAY, '一食堂西门', NULL, NOW() - INTERVAL 3 DAY, NULL, NULL),
  ('SO20251121002', 'pickup', NULL, NULL, '839201', NULL, NOW() - INTERVAL 50 MINUTE, 1);

INSERT INTO payment_record (order_no, account_type, user_id, account_identifier, payment_type, amount,
                            balance_snapshot, status, payment_method, external_no, remark) VALUES
  ('SO20251112001', 'user',     1, 1, 'payment', 45.00, 210.00, 'success', 'wallet', 'PM20251112000001', '购买 Keychron K8'),
  ('SO20251112001', 'platform', NULL, 0, 'payment', 45.00, 45.00, 'success', 'wallet', 'PM20251112000002', '托管收款 SO20251112001'),
  ('SO20251112001', 'platform', NULL, 0, 'payout', 45.00, 0.00, 'success', 'wallet', 'PM20251112000003', '放款 SO20251112001'),
  ('SO20251112001', 'user',     2, 2, 'payout', 45.00, 56.50, 'success', 'wallet', 'PM20251112000004', '出售 Keychron K8'),
  ('SO20251121002', 'user',     3, 3, 'payment', 22.00, 120.00, 'success', 'wallet', 'PM20251121000001', '购买 JBL Go3'),
  ('SO20251121002', 'platform', NULL, 0, 'payment', 22.00, 22.00, 'success', 'wallet', 'PM20251121000002', '托管收款 SO20251121002');

INSERT INTO review (order_no, reviewer_id, target_user_id, rating, tags, content, status, created_at) VALUES
  ('SO20251112001', 1, 2, 5, '["描述相符","发货很快"]', '键盘很新，卖家很好沟通。', 'valid', NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE),
  ('SO20251112001', 2, 1, 5, '["爽快","准时"]', '买家很爽快，面交顺利。', 'valid', NOW() - INTERVAL 3 DAY + INTERVAL 25 MINUTE);

INSERT INTO credit_record (user_id, change_value, reason, order_no, created_at) VALUES
  (1, 2, 'order_complete', 'SO20251112001', NOW() - INTERVAL 3 DAY),
  (2, 2, 'order_complete', 'SO20251112001', NOW() - INTERVAL 3 DAY);

INSERT INTO favorite (user_id, item_id) VALUES (1, 2), (1, 4), (3, 3);

INSERT INTO bounty (buyer_id, category_id, keyword, budget, condition_code, price_tolerance,
                    status, match_count, expires_at) VALUES
  (1, 12, '高等数学', 20.00, 'good', 20.00, 'open', 0, NOW() + INTERVAL 7 DAY),
  (3, 52, '尤克里里', 100.00, 'like_new', 25.00, 'open', 0, NOW() + INTERVAL 5 DAY);

INSERT INTO audit_record (item_id, auditor_id, result, reason, created_at) VALUES
  (2, 10, 'approved', '信息完整，图片清晰', NOW() - INTERVAL 5 DAY),
  (11, 10, 'approved', '信息完整', NOW() - INTERVAL 2 DAY);

INSERT INTO notification (receiver_id, notification_type, related_type, related_id, content, is_read, created_at) VALUES
  (1, 'order_completed', 'order', 'SO20251112001', '交易完成，记得给卖家评价。', 0, NOW() - INTERVAL 3 DAY),
  (2, 'audit', 'item', '2', '你的物品「iPhone 12 128G 蓝色」已审核通过并上架。', 1, NOW() - INTERVAL 5 DAY),
  (1, 'bounty_match', 'bounty', '1', '你求购的「高等数学」已有匹配物品上架。', 0, NOW() - INTERVAL 1 DAY),
  (3, 'verification', NULL, NULL, '学生认证已通过，欢迎使用拾光集。', 1, NOW() - INTERVAL 25 DAY);

-- 一条待处理仲裁，供运营后台演示处理流程
INSERT INTO arbitration (applicant_id, object_type, object_id, arbitration_type, evidence, status) VALUES
  (1, 'order', 'SO20251112001', '虚假描述',
   '键盘右下角有明显磕碰，与「九成新」的描述不符，申请退货退款。', 'pending');
