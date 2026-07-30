-- ============================================================
-- 权益发放记录表（任务6：权益发放记录）
-- ============================================================

CREATE TABLE `benefit_grant_log` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT NOT NULL COMMENT '用户ID',
  `benefit_id`    BIGINT NOT NULL COMMENT '权益ID',
  `benefit_type`  TINYINT NOT NULL COMMENT '权益类型(冗余)',
  `grant_value`   DECIMAL(10,2) DEFAULT NULL COMMENT '发放值(金额/积分)',
  `grant_reason`  VARCHAR(255)  DEFAULT NULL COMMENT '发放原因',
  `order_id`      BIGINT DEFAULT NULL COMMENT '关联订单ID(可选)',
  `operator_id`   BIGINT DEFAULT NULL COMMENT '操作人ID(系统=0)',
  `status`        TINYINT DEFAULT 1 COMMENT '1已发放 2已撤销',
  `grant_time`    DATETIME NOT NULL COMMENT '发放时间',
  `expire_time`   DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_benefit_id` (`benefit_id`),
  KEY `idx_grant_time` (`grant_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益发放记录';
