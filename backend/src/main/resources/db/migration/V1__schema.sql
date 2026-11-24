-- =========================================================
-- 拾光集 · 校园二手交易平台 · V1 schema
-- MySQL 8.0.16+
-- =========================================================

CREATE TABLE student_roster (
  student_no   VARCHAR(20)  NOT NULL COMMENT '学号',
  name         VARCHAR(32)  NOT NULL COMMENT '姓名',
  college      VARCHAR(64)  NOT NULL COMMENT '学院',
  major        VARCHAR(64)  NOT NULL COMMENT '专业',
  grade        VARCHAR(10)  NOT NULL COMMENT '年级',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_no),
  KEY idx_roster_college (college),
  KEY idx_roster_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学籍表';

CREATE TABLE category (
  category_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类号',
  name         VARCHAR(64)     NOT NULL COMMENT '分类名',
  parent_id    BIGINT UNSIGNED NULL COMMENT '父分类号',
  enabled      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
  parent_key   BIGINT UNSIGNED GENERATED ALWAYS AS (IFNULL(parent_id, 0)) STORED COMMENT '父级键，用于同级唯一',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_category_parent_name (parent_key, name),
  KEY idx_category_parent (parent_id),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (category_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_category_enabled CHECK (enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表';

CREATE TABLE pickup_point (
  pickup_point_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自提点号',
  name            VARCHAR(64)     NOT NULL COMMENT '名称',
  address         VARCHAR(128)    NOT NULL COMMENT '地址',
  coordinate      VARCHAR(64)     NULL COMMENT '坐标',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (pickup_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自提点表';

CREATE TABLE platform_account (
  account_no      BIGINT UNSIGNED NOT NULL COMMENT '平台账户号，固定 1',
  escrow_balance  DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '托管余额',
  total_inflow    DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '累计流入',
  total_outflow   DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '累计流出',
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (account_no),
  CONSTRAINT ck_platform_balance CHECK (escrow_balance >= 0 AND total_inflow >= 0 AND total_outflow >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台托管账户（单行）';

CREATE TABLE app_user (
  user_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户号',
  username       VARCHAR(32)     NOT NULL COMMENT '登录用户名',
  password_hash  VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname       VARCHAR(32)     NOT NULL COMMENT '昵称',
  student_no     VARCHAR(20)     NULL COMMENT '绑定学号',
  role           VARCHAR(16)     NOT NULL DEFAULT 'student' COMMENT '角色',
  verify_status  VARCHAR(16)     NOT NULL DEFAULT 'unverified' COMMENT '认证状态',
  credit_score   INT             NOT NULL DEFAULT 100 COMMENT '信用分',
  banned         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否封禁',
  banned_until   DATETIME        NULL COMMENT '封禁到期时间',
  contact        VARCHAR(64)     NULL COMMENT '联系方式',
  balance        DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '余额，可为负',
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_student_no (student_no),
  KEY idx_user_role (role),
  KEY idx_user_verify (verify_status),
  CONSTRAINT fk_user_student FOREIGN KEY (student_no) REFERENCES student_roster (student_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_user_role CHECK (role IN ('student','operator','admin')),
  CONSTRAINT ck_user_verify CHECK (verify_status IN ('unverified','verified','rejected')),
  CONSTRAINT ck_user_credit CHECK (credit_score >= 0),
  CONSTRAINT ck_user_banned CHECK (banned IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE verification_record (
  verification_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '认证号',
  user_id              BIGINT UNSIGNED NOT NULL COMMENT '用户号',
  method               VARCHAR(16)     NOT NULL DEFAULT 'manual' COMMENT '认证方式',
  submitted_student_no VARCHAR(20)     NOT NULL COMMENT '提交学号',
  submitted_name       VARCHAR(32)     NOT NULL COMMENT '提交姓名',
  submitted_college    VARCHAR(64)     NULL COMMENT '提交学院',
  submitted_major      VARCHAR(64)     NULL COMMENT '提交专业',
  submitted_grade      VARCHAR(10)     NULL COMMENT '提交年级',
  result               VARCHAR(16)     NOT NULL COMMENT '核验结果',
  reject_reason        VARCHAR(255)    NULL COMMENT '驳回原因',
  applied_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  verified_at          DATETIME        NULL COMMENT '核验时间',
  created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (verification_id),
  KEY idx_verification_user (user_id, applied_at),
  KEY idx_verification_result (result),
  CONSTRAINT fk_verification_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_verification_result CHECK (result IN ('approved','rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证记录表';

CREATE TABLE item (
  item_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物品编号',
  title             VARCHAR(120)    NOT NULL COMMENT '标题',
  category_id       BIGINT UNSIGNED NOT NULL COMMENT '分类号',
  current_holder_id BIGINT UNSIGNED NOT NULL COMMENT '当前持有人',
  price             DECIMAL(10,2)   NOT NULL COMMENT '售价',
  original_price    DECIMAL(10,2)   NOT NULL COMMENT '原价',
  purchase_price    DECIMAL(10,2)   NULL COMMENT '入手价',
  condition_code    VARCHAR(16)     NOT NULL COMMENT '成色',
  description       TEXT            NULL COMMENT '描述',
  status            VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT '物品状态',
  source            VARCHAR(16)     NOT NULL DEFAULT 'offline' COMMENT '来源',
  notes_tag         VARCHAR(20)     NOT NULL DEFAULT 'no_notes' COMMENT '笔记标签',
  listed_at         DATETIME        NULL COMMENT '上架时间',
  reject_reason     VARCHAR(255)    NULL COMMENT '驳回原因',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (item_id),
  KEY idx_item_status_category (status, category_id, created_at),
  KEY idx_item_holder (current_holder_id, status),
  KEY idx_item_price (price),
  CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category (category_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_item_holder FOREIGN KEY (current_holder_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_item_status CHECK (status IN ('pending','on_sale','locked','completed','rejected','off_shelf','frozen')),
  CONSTRAINT ck_item_condition CHECK (condition_code IN ('new','like_new','good','fair','poor')),
  CONSTRAINT ck_item_source CHECK (source IN ('platform','offline')),
  CONSTRAINT ck_item_notes CHECK (notes_tag IN ('with_notes','with_answers','no_notes')),
  CONSTRAINT ck_item_price CHECK (price >= 0 AND original_price >= 0 AND (purchase_price IS NULL OR purchase_price >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物品表';

CREATE TABLE item_image (
  image_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片号',
  item_id           BIGINT UNSIGNED NOT NULL COMMENT '物品编号',
  path              VARCHAR(255)    NOT NULL COMMENT '图片路径/URL',
  sort_order        INT             NOT NULL DEFAULT 0 COMMENT '排序',
  is_cover          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否首图',
  status            VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT '有效/历史',
  active_cover_item BIGINT UNSIGNED GENERATED ALWAYS AS
    (IF(is_cover = 1 AND status = 'active', item_id, NULL)) STORED COMMENT '有效首图键',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (image_id),
  UNIQUE KEY uk_item_image_active_cover (active_cover_item),
  KEY idx_item_image_item (item_id, status),
  CONSTRAINT fk_item_image_item FOREIGN KEY (item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_item_image_cover CHECK (is_cover IN (0,1)),
  CONSTRAINT ck_item_image_status CHECK (status IN ('active','history'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物品图片表';

CREATE TABLE bounty (
  bounty_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '悬赏号',
  buyer_id        BIGINT UNSIGNED NOT NULL COMMENT '求购人',
  category_id     BIGINT UNSIGNED NOT NULL COMMENT '分类号',
  keyword         VARCHAR(64)     NOT NULL COMMENT '关键词',
  budget          DECIMAL(10,2)   NOT NULL COMMENT '预算',
  condition_code  VARCHAR(16)     NOT NULL COMMENT '期望成色',
  price_tolerance DECIMAL(5,2)    NOT NULL DEFAULT 20.00 COMMENT '价格容差 %',
  status          VARCHAR(16)     NOT NULL DEFAULT 'open' COMMENT '状态',
  match_count     INT             NOT NULL DEFAULT 0 COMMENT '命中次数',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at      DATETIME        NOT NULL COMMENT '有效期',
  closed_at       DATETIME        NULL COMMENT '关闭/过期时间',
  PRIMARY KEY (bounty_id),
  KEY idx_bounty_status_expires (status, expires_at),
  KEY idx_bounty_buyer (buyer_id, status),
  CONSTRAINT fk_bounty_buyer FOREIGN KEY (buyer_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_bounty_category FOREIGN KEY (category_id) REFERENCES category (category_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_bounty_status CHECK (status IN ('open','matched','closed','expired')),
  CONSTRAINT ck_bounty_condition CHECK (condition_code IN ('new','like_new','good','fair','poor')),
  CONSTRAINT ck_bounty_amount CHECK (budget >= 0 AND price_tolerance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='悬赏单表';

CREATE TABLE exchange_intent (
  intent_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '意向号',
  user_id            BIGINT UNSIGNED NOT NULL COMMENT '用户号',
  my_item_id         BIGINT UNSIGNED NOT NULL COMMENT '我的物品',
  match_type         VARCHAR(16)     NOT NULL COMMENT '指定物品/条件匹配',
  target_item_id     BIGINT UNSIGNED NULL COMMENT '期望物品',
  target_category_id BIGINT UNSIGNED NULL COMMENT '期望分类',
  keyword            VARCHAR(64)     NULL COMMENT '期望关键词',
  status             VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT '状态',
  active_item_id     BIGINT UNSIGNED GENERATED ALWAYS AS
    (IF(status = 'active', my_item_id, NULL)) STORED COMMENT '有效意向物品键',
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (intent_id),
  UNIQUE KEY uk_exchange_intent_active_item (active_item_id),
  KEY idx_exchange_intent_user (user_id, status),
  CONSTRAINT fk_intent_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_intent_my_item FOREIGN KEY (my_item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_intent_target_item FOREIGN KEY (target_item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_intent_target_category FOREIGN KEY (target_category_id) REFERENCES category (category_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_intent_match CHECK (
    (match_type = 'specific' AND target_item_id IS NOT NULL)
    OR
    (match_type = 'condition' AND (target_category_id IS NOT NULL OR (keyword IS NOT NULL AND keyword <> '')))
  ),
  CONSTRAINT ck_intent_status CHECK (status IN ('active','matched','cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='置换意向表';

CREATE TABLE exchange_plan (
  plan_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '方案号',
  ring_type       TINYINT UNSIGNED NOT NULL COMMENT '2 人 / 3 人',
  intent_a_id     BIGINT UNSIGNED NOT NULL COMMENT '意向 A',
  intent_b_id     BIGINT UNSIGNED NULL COMMENT '意向 B',
  intent_c_id     BIGINT UNSIGNED NULL COMMENT '意向 C',
  status          VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT '状态',
  confirm_deadline DATETIME       NOT NULL COMMENT '确认截止',
  confirmed_a_at  DATETIME        NULL COMMENT 'A 确认时间',
  confirmed_b_at  DATETIME        NULL COMMENT 'B 确认时间',
  confirmed_c_at  DATETIME        NULL COMMENT 'C 确认时间',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id),
  KEY idx_exchange_plan_status (status, confirm_deadline),
  CONSTRAINT fk_plan_intent_a FOREIGN KEY (intent_a_id) REFERENCES exchange_intent (intent_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_plan_intent_b FOREIGN KEY (intent_b_id) REFERENCES exchange_intent (intent_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_plan_intent_c FOREIGN KEY (intent_c_id) REFERENCES exchange_intent (intent_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_plan_ring CHECK (ring_type IN (2,3)),
  CONSTRAINT ck_plan_participants CHECK (
    (ring_type = 2 AND intent_b_id IS NULL AND intent_c_id IS NULL)
    OR
    (ring_type = 3 AND intent_b_id IS NOT NULL AND intent_c_id IS NOT NULL)
  ),
  CONSTRAINT ck_plan_status CHECK (status IN ('pending','active','void','completed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='置换方案表';

CREATE TABLE trade_order (
  order_no           VARCHAR(32)     NOT NULL COMMENT '订单号',
  order_type         VARCHAR(16)     NOT NULL DEFAULT 'normal' COMMENT '普通/置换',
  buyer_id           BIGINT UNSIGNED NOT NULL COMMENT '买家快照',
  seller_id          BIGINT UNSIGNED NOT NULL COMMENT '卖家快照',
  item_id            BIGINT UNSIGNED NOT NULL COMMENT '物品编号',
  amount             DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '金额',
  delivery_method    VARCHAR(16)     NOT NULL COMMENT '面交/自提',
  pickup_point_id    BIGINT UNSIGNED NULL COMMENT '自提点',
  trade_status       VARCHAR(24)     NOT NULL DEFAULT 'pending_payment' COMMENT '交易状态',
  after_sale_status  VARCHAR(16)     NOT NULL DEFAULT 'none' COMMENT '售后状态',
  source_bounty_id   BIGINT UNSIGNED NULL COMMENT '来源悬赏',
  plan_id            BIGINT UNSIGNED NULL COMMENT '置换方案',
  pay_deadline       DATETIME        NULL COMMENT '支付截止',
  confirm_deadline   DATETIME        NULL COMMENT '自动确认截止',
  paid_at            DATETIME        NULL,
  completed_at       DATETIME        NULL,
  cancelled_at       DATETIME        NULL,
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (order_no),
  KEY idx_order_buyer (buyer_id, created_at),
  KEY idx_order_seller (seller_id, created_at),
  KEY idx_order_item (item_id),
  KEY idx_order_status (trade_status),
  KEY idx_order_after_sale (after_sale_status),
  KEY idx_order_plan (plan_id),
  KEY idx_order_bounty (source_bounty_id),
  CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_item FOREIGN KEY (item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_pickup FOREIGN KEY (pickup_point_id) REFERENCES pickup_point (pickup_point_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_bounty FOREIGN KEY (source_bounty_id) REFERENCES bounty (bounty_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_plan FOREIGN KEY (plan_id) REFERENCES exchange_plan (plan_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_order_type CHECK (order_type IN ('normal','exchange')),
  CONSTRAINT ck_order_delivery CHECK (delivery_method IN ('meetup','pickup')),
  CONSTRAINT ck_order_trade_status CHECK (trade_status IN ('pending_payment','pending_delivery','pending_confirm','completed','cancelled')),
  CONSTRAINT ck_order_after_sale CHECK (after_sale_status IN ('none','refunding','arbitrating','refunded')),
  CONSTRAINT ck_order_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE payment_record (
  payment_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水号',
  order_no          VARCHAR(32)     NULL COMMENT '订单号，充值可空',
  account_type      VARCHAR(16)     NOT NULL COMMENT '用户/平台',
  user_id           BIGINT UNSIGNED NULL COMMENT '用户号，平台流水为空',
  account_identifier BIGINT UNSIGNED NOT NULL COMMENT '账户标识：用户号/平台账户号',
  payment_type      VARCHAR(16)     NOT NULL COMMENT '充值/支付/放款/退款/扣回',
  amount            DECIMAL(10,2)   NOT NULL COMMENT '金额',
  balance_snapshot  DECIMAL(14,2)   NOT NULL COMMENT '对应账户余额快照',
  status            VARCHAR(16)     NOT NULL DEFAULT 'success' COMMENT '状态',
  payment_method    VARCHAR(16)     NOT NULL DEFAULT 'wallet' COMMENT '支付方式',
  external_no       VARCHAR(64)     NOT NULL COMMENT '外部单号',
  remark            VARCHAR(255)    NULL COMMENT '备注',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  UNIQUE KEY uk_payment_external (external_no),
  UNIQUE KEY uk_payment_order_type_account (order_no, payment_type, account_identifier),
  KEY idx_payment_user (user_id, created_at),
  KEY idx_payment_order (order_no),
  KEY idx_payment_account (account_identifier),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_no) REFERENCES trade_order (order_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_payment_account CHECK (account_type IN ('user','platform')),
  CONSTRAINT ck_payment_type CHECK (payment_type IN ('recharge','payment','payout','refund','chargeback')),
  CONSTRAINT ck_payment_status CHECK (status IN ('success','failed')),
  CONSTRAINT ck_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

CREATE TABLE delivery_record (
  delivery_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录号',
  order_no        VARCHAR(32)     NOT NULL COMMENT '订单号',
  method          VARCHAR(16)     NOT NULL COMMENT '面交/自提',
  agreed_time     DATETIME        NULL COMMENT '约定时间',
  meetup_place    VARCHAR(128)    NULL COMMENT '面交地点',
  pickup_code     VARCHAR(16)     NULL COMMENT '取件码',
  verify_time     DATETIME        NULL COMMENT '核验时间',
  inbound_time    DATETIME        NULL COMMENT '入库时间',
  outbound_time   DATETIME        NULL COMMENT '出库时间',
  pickup_point_id BIGINT UNSIGNED NULL COMMENT '自提点',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (delivery_id),
  UNIQUE KEY uk_delivery_pickup_code (pickup_code),
  KEY idx_delivery_order (order_no),
  KEY idx_delivery_pickup_point (pickup_point_id),
  CONSTRAINT fk_delivery_order FOREIGN KEY (order_no) REFERENCES trade_order (order_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_delivery_pickup FOREIGN KEY (pickup_point_id) REFERENCES pickup_point (pickup_point_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_delivery_method CHECK (method IN ('meetup','pickup'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交付记录表';

CREATE TABLE review (
  review_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价号',
  order_no       VARCHAR(32)     NOT NULL COMMENT '订单号',
  reviewer_id    BIGINT UNSIGNED NOT NULL COMMENT '评价人',
  target_user_id BIGINT UNSIGNED NOT NULL COMMENT '被评人',
  rating         TINYINT UNSIGNED NOT NULL COMMENT '1-5',
  tags           JSON            NULL COMMENT '标签数组',
  content        VARCHAR(500)    NULL COMMENT '内容',
  status         VARCHAR(16)     NOT NULL DEFAULT 'valid' COMMENT '有效/作废/系统默认',
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (review_id),
  UNIQUE KEY uk_review_order_reviewer (order_no, reviewer_id),
  KEY idx_review_target (target_user_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_no) REFERENCES trade_order (order_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_review_target FOREIGN KEY (target_user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT ck_review_status CHECK (status IN ('valid','void','system'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

CREATE TABLE credit_record (
  credit_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录号',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户号',
  change_value INT             NOT NULL COMMENT '变动值',
  reason       VARCHAR(32)     NOT NULL COMMENT '原因',
  order_no     VARCHAR(32)     NULL COMMENT '关联订单',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (credit_id),
  UNIQUE KEY uk_credit_user_order_reason (user_id, order_no, reason),
  KEY idx_credit_user (user_id, created_at),
  CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_credit_order FOREIGN KEY (order_no) REFERENCES trade_order (order_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_credit_change CHECK (change_value <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用记录表';

CREATE TABLE item_history (
  history_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '履历号',
  item_id         BIGINT UNSIGNED NOT NULL COMMENT '物品编号',
  event_type      VARCHAR(16)     NOT NULL COMMENT '事件类型',
  event_time      DATETIME        NOT NULL COMMENT '事件时间',
  description     VARCHAR(255)    NOT NULL COMMENT '描述',
  related_user_id BIGINT UNSIGNED NULL COMMENT '关联用户',
  order_no        VARCHAR(32)     NULL COMMENT '关联订单',
  message         VARCHAR(255)    NULL COMMENT '卖家寄语',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (history_id),
  KEY idx_history_item (item_id, event_time),
  KEY idx_history_order (order_no),
  CONSTRAINT fk_history_item FOREIGN KEY (item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_history_order FOREIGN KEY (order_no) REFERENCES trade_order (order_no)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_history_user FOREIGN KEY (related_user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_history_event CHECK (event_type IN ('created','deal','exchange','return','repair'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履历记录表';

CREATE TABLE notification (
  notification_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知号',
  receiver_id      BIGINT UNSIGNED NOT NULL COMMENT '接收人',
  notification_type VARCHAR(32)    NOT NULL COMMENT '通知类型',
  related_type     VARCHAR(32)     NULL COMMENT '多态类型',
  related_id       VARCHAR(64)     NULL COMMENT '多态编号',
  content          VARCHAR(500)    NOT NULL COMMENT '内容',
  is_read          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已读',
  read_at          DATETIME        NULL COMMENT '阅读时间',
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notification_receiver (receiver_id, is_read, created_at),
  CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_notification_read CHECK (is_read IN (0,1)),
  CONSTRAINT ck_notification_type CHECK (notification_type IN (
    'verification','audit','bounty_match','exchange_invite',
    'aftersale_result','arbitration_accepted','arbitration_result','order_completed'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';

CREATE TABLE favorite (
  favorite_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏号',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户号',
  item_id     BIGINT UNSIGNED NOT NULL COMMENT '物品编号',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (favorite_id),
  UNIQUE KEY uk_favorite_user_item (user_id, item_id),
  KEY idx_favorite_item (item_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_favorite_item FOREIGN KEY (item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';

CREATE TABLE audit_record (
  audit_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审核号',
  item_id     BIGINT UNSIGNED NOT NULL COMMENT '物品编号',
  auditor_id  BIGINT UNSIGNED NOT NULL COMMENT '审核人',
  result      VARCHAR(16)     NOT NULL COMMENT '通过/驳回',
  reason      VARCHAR(255)    NULL COMMENT '理由',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_id),
  KEY idx_audit_item (item_id, created_at),
  KEY idx_audit_auditor (auditor_id),
  CONSTRAINT fk_audit_item FOREIGN KEY (item_id) REFERENCES item (item_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_audit_auditor FOREIGN KEY (auditor_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_audit_result CHECK (result IN ('approved','rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核记录表';

CREATE TABLE arbitration (
  arbitration_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '仲裁号',
  applicant_id     BIGINT UNSIGNED NOT NULL COMMENT '申请人',
  object_type      VARCHAR(16)     NOT NULL COMMENT '对象类型',
  object_id        VARCHAR(64)     NOT NULL COMMENT '对象编号',
  arbitration_type VARCHAR(32)     NOT NULL COMMENT '仲裁类型',
  evidence         VARCHAR(1000)   NULL COMMENT '举证',
  status           VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT '状态',
  result           VARCHAR(16)     NULL COMMENT '裁定结果',
  handler_id       BIGINT UNSIGNED NULL COMMENT '处理人',
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at      DATETIME        NULL COMMENT '处理时间',
  PRIMARY KEY (arbitration_id),
  KEY idx_arbitration_status (status, created_at),
  KEY idx_arbitration_applicant (applicant_id),
  KEY idx_arbitration_object (object_type, object_id),
  CONSTRAINT fk_arbitration_applicant FOREIGN KEY (applicant_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_arbitration_handler FOREIGN KEY (handler_id) REFERENCES app_user (user_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_arbitration_object CHECK (object_type IN ('order','item','user','review')),
  CONSTRAINT ck_arbitration_status CHECK (status IN ('pending','resolved')),
  CONSTRAINT ck_arbitration_result CHECK (result IS NULL OR result IN ('buyer','seller'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仲裁申请表';
