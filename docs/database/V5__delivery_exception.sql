-- ============================================================
-- 配送异常表（任务8：配送异常处理）
-- ============================================================

CREATE TABLE `delivery_exception` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_id`      BIGINT NOT NULL COMMENT '配送单ID',
  `order_id`         BIGINT NOT NULL COMMENT '订单ID',
  `exception_type`   TINYINT NOT NULL COMMENT '异常类型 1配送超时 2地址错误 3联系不上 4商品损坏 5其他',
  `exception_desc`   VARCHAR(500) DEFAULT NULL COMMENT '异常描述',
  `reported_by`      BIGINT NOT NULL COMMENT '上报人ID',
  `report_time`      DATETIME NOT NULL COMMENT '上报时间',
  `handle_status`    TINYINT DEFAULT 0 COMMENT '处理状态 0待处理 1处理中 2已解决 3已关闭',
  `handler_id`       BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handle_time`      DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_result`    VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_delivery_id` (`delivery_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送异常记录';

-- delivery_order 表增加异常标记
ALTER TABLE `delivery_order`
  ADD COLUMN `has_exception` TINYINT DEFAULT 0 COMMENT '是否有异常' AFTER `remark`;
