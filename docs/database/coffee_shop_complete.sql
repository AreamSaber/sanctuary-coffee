SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS `coffee_shop`;
CREATE DATABASE `coffee_shop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `coffee_shop`;


CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `gender` TINYINT DEFAULT 0,
  `birthday` DATE DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL,
  `role_code` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0,
  `permission_name` VARCHAR(50) NOT NULL,
  `permission_code` VARCHAR(100) NOT NULL,
  `permission_type` TINYINT DEFAULT 1,
  `path` VARCHAR(255) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `district` VARCHAR(50) NOT NULL,
  `detail_address` VARCHAR(255) NOT NULL,
  `is_default` TINYINT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_behavior` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `session_id` VARCHAR(100) DEFAULT NULL,
  `action_type` VARCHAR(20) NOT NULL,
  `target_type` VARCHAR(20) DEFAULT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `action_data` TEXT DEFAULT NULL,
  `page_url` VARCHAR(500) DEFAULT NULL,
  `referrer` VARCHAR(500) DEFAULT NULL,
  `device_type` VARCHAR(20) DEFAULT NULL,
  `os` VARCHAR(50) DEFAULT NULL,
  `browser` VARCHAR(50) DEFAULT NULL,
  `ip_address` VARCHAR(50) DEFAULT NULL,
  `location` VARCHAR(100) DEFAULT NULL,
  `duration` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `product_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0,
  `category_name` VARCHAR(50) NOT NULL,
  `category_code` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `level` INT DEFAULT 1,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT NOT NULL,
  `product_name` VARCHAR(100) NOT NULL,
  `product_code` VARCHAR(50) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `main_image` VARCHAR(255) DEFAULT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `original_price` DECIMAL(10, 2) DEFAULT NULL,
  `cost_price` DECIMAL(10, 2) DEFAULT NULL,
  `stock` INT DEFAULT 0,
  `locked_stock` INT DEFAULT 0 COMMENT '锁定库存（已下单未支付）',
  `sales` INT DEFAULT 0,
  unit VARCHAR(20) DEFAULT 'item',
  `status` TINYINT DEFAULT 1,
  `is_hot` TINYINT DEFAULT 0,
  `is_new` TINYINT DEFAULT 0,
  `is_recommend` TINYINT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `sort_order` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_spec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `spec_name` VARCHAR(50) NOT NULL,
  `spec_values` VARCHAR(255) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_sku` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `sku_code` VARCHAR(50) NOT NULL,
  `sku_name` VARCHAR(100) NOT NULL,
  `spec_info` VARCHAR(255) DEFAULT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `stock` INT DEFAULT 0,
  `locked_stock` INT DEFAULT 0 COMMENT '锁定库存（已下单未支付）',
  `image` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_stock_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `sku_id` BIGINT DEFAULT NULL,
  `change_type` TINYINT NOT NULL,
  `change_quantity` INT NOT NULL,
  `before_stock` INT NOT NULL,
  `after_stock` INT NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `rating` TINYINT NOT NULL,
  `content` TEXT DEFAULT NULL,
  `images` VARCHAR(500) DEFAULT NULL,
  `is_anonymous` TINYINT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `review_reply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `review_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `shopping_cart` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `sku_id` BIGINT DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `checked` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(10, 2) NOT NULL,
  `pay_amount` DECIMAL(10, 2) NOT NULL,
  `discount_amount` DECIMAL(10, 2) DEFAULT 0,
  `freight_amount` DECIMAL(10, 2) DEFAULT 0,
  `delivery_method_id` BIGINT DEFAULT NULL,
  `delivery_method_name` VARCHAR(50) DEFAULT NULL,
  `order_status` TINYINT NOT NULL DEFAULT 1,
  `pay_status` TINYINT DEFAULT 0,
  `pay_type` TINYINT DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT NULL,
  `payment_method` VARCHAR(20) DEFAULT NULL,
  `pay_time` DATETIME DEFAULT NULL,
  `delivery_time` DATETIME DEFAULT NULL,
  `receive_time` DATETIME DEFAULT NULL,
  `cancel_time` DATETIME DEFAULT NULL,
  `cancel_reason` VARCHAR(255) DEFAULT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `receiver_address` VARCHAR(255) NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `sku_id` BIGINT DEFAULT NULL,
  `product_name` VARCHAR(100) NOT NULL,
  `product_image` VARCHAR(255) DEFAULT NULL,
  `spec_info` VARCHAR(255) DEFAULT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `quantity` INT NOT NULL,
  `total_amount` DECIMAL(10, 2) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_after_sale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `after_sale_no` VARCHAR(50) NOT NULL,
  `type` TINYINT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `images` VARCHAR(500) DEFAULT NULL,
  `refund_amount` DECIMAL(10, 2) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `handle_time` DATETIME DEFAULT NULL,
  `handle_remark` VARCHAR(255) DEFAULT NULL,
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_after_sale_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `after_sale_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_type` VARCHAR(20) NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `status_from` TINYINT DEFAULT NULL,
  `status_to` TINYINT DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_id` (`after_sale_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `delivery_method` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `method_name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `base_freight` DECIMAL(10, 2) DEFAULT 0,
  `free_freight_amount` DECIMAL(10, 2) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `delivery_region` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0,
  `region_name` VARCHAR(100) NOT NULL,
  `region_code` VARCHAR(50) NOT NULL,
  `level` INT DEFAULT 1,
  `delivery_fee` DECIMAL(10, 2) DEFAULT 0,
  `min_order_amount` DECIMAL(10, 2) DEFAULT 0,
  `estimated_time` INT DEFAULT 30,
  `longitude` DECIMAL(10, 6) DEFAULT NULL,
  `latitude` DECIMAL(10, 6) DEFAULT NULL,
  `delivery_range` DECIMAL(5, 1) DEFAULT 0,
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_code` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `delivery_staff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `region_id` BIGINT DEFAULT NULL,
  `staff_code` VARCHAR(50) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `id_number` VARCHAR(18) DEFAULT NULL,
  `vehicle_type` VARCHAR(20) DEFAULT NULL,
  `vehicle_number` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'OFFLINE',
  `today_orders` INT DEFAULT 0,
  `total_orders` INT DEFAULT 0,
  `rating` DECIMAL(3, 1) DEFAULT 5.0,
  `join_time` DATETIME DEFAULT NULL,
  `health_cert_no` VARCHAR(100) DEFAULT NULL,
  `health_cert_expiry` DATE DEFAULT NULL,
  `enabled` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_code` (`staff_code`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `delivery_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `delivery_no` VARCHAR(50) NOT NULL,
  `staff_id` BIGINT DEFAULT NULL,
  `delivery_status` TINYINT DEFAULT 1,
  `assign_time` DATETIME DEFAULT NULL,
  `pickup_time` DATETIME DEFAULT NULL,
  `delivery_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `has_exception` TINYINT DEFAULT 0 COMMENT '是否有异常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_no` (`delivery_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_staff_id` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `delivery_exception` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_id` BIGINT NOT NULL COMMENT '配送单ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `exception_type` TINYINT NOT NULL COMMENT '异常类型 1配送超时 2地址错误 3联系不上 4商品损坏 5其他',
  `exception_desc` VARCHAR(500) DEFAULT NULL COMMENT '异常描述',
  `reported_by` BIGINT NOT NULL COMMENT '上报人ID',
  `report_time` DATETIME NOT NULL COMMENT '上报时间',
  `handle_status` TINYINT DEFAULT 0 COMMENT '处理状态 0待处理 1处理中 2已解决 3已关闭',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_delivery_id` (`delivery_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送异常记录';

CREATE TABLE `delivery_track` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_order_id` BIGINT NOT NULL,
  `track_info` VARCHAR(255) NOT NULL,
  `longitude` DECIMAL(10, 6) DEFAULT NULL,
  `latitude` DECIMAL(10, 6) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_delivery_order_id` (`delivery_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(50) NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `pay_amount` DECIMAL(10, 2) NOT NULL,
  `pay_type` INT NOT NULL,
  `pay_status` INT NOT NULL DEFAULT 0,
  `trade_no` VARCHAR(100) DEFAULT NULL,
  `pay_time` DATETIME DEFAULT NULL,
  `deleted` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `refund_no` VARCHAR(50) NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `refund_amount` DECIMAL(10, 2) NOT NULL,
  `refund_reason` VARCHAR(500) DEFAULT NULL,
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `refund_status` INT NOT NULL DEFAULT 0,
  `trade_no` VARCHAR(100) DEFAULT NULL,
  `refund_time` DATETIME DEFAULT NULL,
  `deleted` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `financial_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_no` VARCHAR(50) NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `record_type` TINYINT NOT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `balance` DECIMAL(10, 2) NOT NULL,
  `business_type` VARCHAR(50) NOT NULL,
  `business_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_no` (`record_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `invoice_no` VARCHAR(50) NOT NULL,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `invoice_type` TINYINT NOT NULL,
  `title_type` TINYINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `tax_no` VARCHAR(50) DEFAULT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `issue_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_no` (`invoice_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `member_level` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `level_name` VARCHAR(50) NOT NULL,
  `level_no` INT NOT NULL,
  `required_points` INT DEFAULT 0,
  `discount_rate` DECIMAL(3, 2) DEFAULT 1.00,
  `description` VARCHAR(255) DEFAULT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_no` (`level_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `member_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `level_id` BIGINT NOT NULL,
  `points` INT DEFAULT 0,
  `growth_value` INT DEFAULT 0,
  `balance` DECIMAL(10, 2) DEFAULT 0,
  `total_consume` DECIMAL(10, 2) DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `member_benefit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `benefit_name` VARCHAR(100) NOT NULL,
  `benefit_code` VARCHAR(50) NOT NULL,
  `benefit_type` TINYINT NOT NULL COMMENT '1专属折扣 2积分倍率 3免配送费 4生日礼券 5专属服务',
  `benefit_value` DECIMAL(10, 2) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_benefit_code` (`benefit_code`),
  KEY `idx_benefit_type` (`benefit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `member_level_benefit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `level_id` BIGINT NOT NULL,
  `benefit_id` BIGINT NOT NULL,
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_benefit_id` (`benefit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `member_benefit_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `benefit_id` BIGINT NOT NULL,
  `benefit_name` VARCHAR(100) NOT NULL,
  `benefit_type` TINYINT NOT NULL COMMENT '1专属折扣 2积分倍率 3免配送费 4生日礼券 5专属服务',
  `benefit_value` DECIMAL(10, 2) DEFAULT NULL,
  `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型，如PAYMENT_CREATE、PAYMENT_REWARD、REFUND_ROLLBACK',
  `business_id` BIGINT DEFAULT NULL,
  `effect_amount` DECIMAL(10, 2) DEFAULT 0.00,
  `effect_points` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1使用 2回滚/扣回',
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_benefit_business` (`user_id`, `benefit_id`, `business_type`, `business_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_benefit_type` (`benefit_type`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `benefit_grant_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `benefit_id` BIGINT NOT NULL COMMENT '权益ID',
  `benefit_type` TINYINT NOT NULL COMMENT '权益类型(冗余)',
  `grant_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '发放值(金额/积分)',
  `grant_reason` VARCHAR(255) DEFAULT NULL COMMENT '发放原因',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID(可选)',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID(系统=0)',
  `status` TINYINT DEFAULT 1 COMMENT '1已发放 2已撤销',
  `grant_time` DATETIME NOT NULL COMMENT '发放时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_benefit_id` (`benefit_id`),
  KEY `idx_grant_time` (`grant_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益发放记录';

CREATE TABLE `points_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `change_type` TINYINT NOT NULL,
  `change_points` INT NOT NULL,
  `before_points` INT NOT NULL,
  `after_points` INT NOT NULL,
  `source_type` VARCHAR(50) NOT NULL,
  `source_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coupon_name` VARCHAR(100) NOT NULL,
  `coupon_type` TINYINT NOT NULL,
  `discount_type` TINYINT NOT NULL,
  `discount_value` DECIMAL(10, 2) NOT NULL,
  `min_amount` DECIMAL(10, 2) DEFAULT 0,
  `total_quantity` INT DEFAULT NULL,
  `received_quantity` INT DEFAULT 0,
  `per_limit` INT DEFAULT 1,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `valid_days` INT DEFAULT NULL,
  `use_scope` TINYINT DEFAULT 1,
  `scope_ids` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coupon_id` BIGINT NOT NULL,
  `coupon_code` VARCHAR(50) NOT NULL,
  `status` TINYINT DEFAULT 0,
  `use_time` DATETIME DEFAULT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `expire_time` DATETIME NOT NULL,
  `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `promotion_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_name` VARCHAR(100) NOT NULL,
  `activity_type` TINYINT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `description` TEXT DEFAULT NULL,
  `banner` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `promotion_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `promotion_price` DECIMAL(10, 2) NOT NULL,
  `stock_limit` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `sales_statistics_daily` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stat_date` DATE NOT NULL,
  `order_count` INT DEFAULT 0,
  `order_amount` DECIMAL(12, 2) DEFAULT 0,
  `pay_count` INT DEFAULT 0,
  `pay_amount` DECIMAL(12, 2) DEFAULT 0,
  `refund_count` INT DEFAULT 0,
  `refund_amount` DECIMAL(12, 2) DEFAULT 0,
  `new_user_count` INT DEFAULT 0,
  `active_user_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_sales_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL,
  `sales_count` INT DEFAULT 0,
  `sales_amount` DECIMAL(10, 2) DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `collect_count` INT DEFAULT 0,
  `cart_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_date` (`product_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_active_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL,
  `login_count` INT DEFAULT 0,
  `browse_count` INT DEFAULT 0,
  `order_count` INT DEFAULT 0,
  `pay_count` INT DEFAULT 0,
  `online_time` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `operation_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stat_date` DATE NOT NULL,
  `total_user` INT DEFAULT 0,
  `new_user` INT DEFAULT 0,
  `active_user` INT DEFAULT 0,
  `total_order` INT DEFAULT 0,
  `total_amount` DECIMAL(12, 2) DEFAULT 0,
  `avg_order_amount` DECIMAL(10, 2) DEFAULT 0,
  `conversion_rate` DECIMAL(5, 4) DEFAULT 0,
  `repurchase_rate` DECIMAL(5, 4) DEFAULT 0,
  `product_count` INT DEFAULT 0,
  `category_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(50) NOT NULL,
  `config_value` TEXT NOT NULL,
  `config_type` VARCHAR(20) DEFAULT 'string',
  `description` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) DEFAULT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `link_url` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `position` VARCHAR(20) DEFAULT 'home',
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL,
  `content` TEXT NOT NULL,
  `notice_type` TINYINT DEFAULT 1,
  `publish_time` DATETIME DEFAULT NULL,
  `status` TINYINT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`) VALUES
(1, '管理员', 'ROLE_ADMIN', '系统管理员', 1),
(2, '用户', 'ROLE_USER', '普通用户', 1),
(3, '配送员', 'ROLE_DELIVERY', '配送人员', 1);

INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin', '', '系统管理员', 'admin@coffee.com', '13800138000', 1, 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 'test', '', '测试用户', 'test@coffee.com', '13800000000', 1, 1, '2026-03-01 08:10:00', '2026-03-01 08:10:00'),
(3, 'customer1', '', '王明', 'wang@example.com', '13900001111', 1, 1, '2026-03-01 08:20:00', '2026-03-01 08:20:00'),
(4, 'customer2', '', '李华', 'li@example.com', '13900001112', 0, 1, '2026-03-01 08:30:00', '2026-03-01 08:30:00'),
(6, 'delivery1', '', '骑手张强', 'delivery1@example.com', '13900002001', 1, 1, '2026-03-01 08:40:00', '2026-03-01 08:40:00'),
(7, 'delivery2', '', '骑手李雷', 'delivery2@example.com', '13900002002', 1, 1, '2026-03-01 08:50:00', '2026-03-01 08:50:00'),
(20, 'ops_lead', '', '运营主管', 'ops@coffee.com', '13910002020', 1, 1, '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(21, 'finance_desk', '', '财务专员', 'finance@coffee.com', '13910002021', 0, 1, '2026-03-01 09:10:00', '2026-03-01 09:10:00'),
(22, 'member_chen', '', 'Chen', 'chen@coffee.com', '13910002022', 0, 1, '2026-03-02 09:00:00', '2026-03-29 09:00:00'),
(23, 'member_lin', '', 'Lin', 'lin@coffee.com', '13910002023', 0, 1, '2026-03-03 09:30:00', '2026-03-29 09:30:00'),
(24, 'member_qiao', '', 'Qiao', 'qiao@coffee.com', '13910002024', 1, 1, '2026-03-04 10:00:00', '2026-03-29 10:00:00'),
(25, 'member_sun', '', 'Sun', 'sun@coffee.com', '13910002025', 1, 1, '2026-03-05 10:30:00', '2026-03-29 10:30:00');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 2), (4, 2), (6, 3), (7, 3),
(20, 1), (21, 1), (22, 2), (23, 2), (24, 2), (25, 2);

INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `path`, `icon`, `sort_order`, `status`) VALUES
(1, 0, '后台治理', 'admin', 1, '/admin', 'setting', 1, 1),
(2, 1, '后台工作台', 'admin:workbench:view', 2, '/admin', 'house', 1, 1),
(3, 1, 'RBAC 权限后台', 'rbac:view', 2, '/admin/rbac', 'user', 2, 1),
(10, 0, '用户与会员', 'customer', 1, '/member', 'user', 2, 1),
(11, 10, '用户中心', 'user:center:view', 2, '/user', 'user', 1, 1),
(12, 10, '地址管理', 'user:address:view', 2, '/user/address', 'location', 2, 1),
(13, 10, '会员管理', 'member:manage', 2, '/member', 'user', 3, 1),
(14, 10, '会员权益', 'member:benefit', 2, '/member/benefits', 'present', 4, 1),
(15, 10, '用户行为分析', 'user:analytics:view', 2, '/analytics', 'trend', 5, 1),
(20, 0, '商品与库存', 'catalog', 1, '/product', 'goods', 3, 1),
(21, 20, '商品管理', 'product:list', 2, '/product', 'goods', 1, 1),
(22, 20, '分类管理', 'product:category', 2, '/product/category', 'menu', 2, 1),
(23, 20, '规格 SKU', 'product:sku', 2, '/product', 'grid', 3, 1),
(24, 20, '库存日志', 'product:stock', 2, '/product/stock-warning', 'box', 4, 1),
(25, 20, '库存预警', 'product:warning', 2, '/product/stock-warning', 'warning', 5, 1),
(30, 0, '交易与财务', 'trade', 1, '/order', 'list', 4, 1),
(31, 30, '我的订单', 'order:member', 2, '/order', 'list', 1, 1),
(32, 30, '订单管理', 'order:manage', 2, '/order/manage', 'list', 2, 1),
(33, 30, '退款管理', 'order:refund', 2, '/payment/refund', 'document', 3, 1),
(34, 30, '售后管理', 'order:after-sale', 2, '/order/after-sale', 'service', 4, 1),
(35, 30, '支付与结算', 'payment:manage', 2, '/payment', 'wallet', 5, 1),
(36, 30, '我的发票', 'invoice:self', 2, '/financial/invoice', 'document', 6, 1),
(37, 30, '发票管理', 'invoice:manage', 2, '/financial/invoice', 'document', 7, 1),
(38, 30, '财务报表', 'financial:report', 2, '/financial/report', 'data', 8, 1),
(40, 0, '配送履约', 'delivery', 1, '/delivery/manage', 'van', 5, 1),
(41, 40, '配送管理', 'delivery:manage', 2, '/delivery/manage', 'van', 1, 1),
(42, 40, '配送区域', 'delivery:region', 2, '/delivery/manage', 'location', 2, 1),
(43, 40, '配送员管理', 'delivery:staff', 2, '/delivery/manage', 'user', 3, 1),
(44, 40, '物流轨迹', 'delivery:tracking', 2, '/delivery/tracking', 'location', 4, 1),
(58, 40, '配送方式', 'delivery:method', 2, '/delivery/manage', 'van', 5, 1),
(50, 0, '营销与运营', 'marketing', 1, '/coupon', 'present', 6, 1),
(51, 50, '领券中心', 'coupon:center', 2, '/coupon', 'ticket', 1, 1),
(52, 50, '优惠券管理', 'coupon:manage', 2, '/coupon/manage', 'ticket', 2, 1),
(53, 50, '促销活动', 'promotion:manage', 2, '/promotion', 'present', 3, 1),
(54, 50, '我的评价', 'review:self', 2, '/review/my', 'chat', 4, 1),
(55, 50, '评价管理', 'review:manage', 2, '/review/manage', 'chat', 5, 1),
(56, 50, '运营看板', 'dashboard:view', 2, '/statistics', 'data', 6, 1),
(57, 50, '运营分析', 'operation:analytics', 2, '/analytics', 'trend', 7, 1);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) SELECT 1, `id` FROM `sys_permission`;
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 11), (2, 12), (2, 31), (2, 35), (2, 36), (2, 44), (2, 51), (2, 54),
(3, 31), (3, 44);

INSERT INTO `product_category` (`id`, `parent_id`, `category_name`, `category_code`, `icon`, `sort_order`, `level`) VALUES
(1, 0, '咖啡', 'coffee', 'coffee.png', 1, 1),
(2, 0, '茶饮', 'tea', 'tea.png', 2, 1),
(3, 0, '甜品', 'dessert', 'dessert.png', 3, 1),
(5, 1, '美式咖啡', 'americano', NULL, 1, 2),
(6, 1, '拿铁', 'latte', NULL, 2, 2),
(9, 2, '奶茶', 'milk-tea', NULL, 1, 2),
(10, 2, '果茶', 'fruit-tea', NULL, 2, 2),
(11, 3, '蛋糕', 'cake', NULL, 1, 2);

INSERT INTO `product` (`id`, `category_id`, `product_name`, `product_code`, `description`, `main_image`, `price`, `original_price`, `cost_price`, `stock`, `sales`, `unit`, `status`, `is_hot`, `is_new`, `is_recommend`) VALUES
(1, 5, '经典美式', 'P001', '口感均衡顺滑的美式咖啡。', '/images/products/americano.jpg', 18.00, 20.00, 8.00, 100, 256, '杯', 1, 1, 0, 1),
(2, 6, '香草拿铁', 'P002', '新鲜牛奶搭配香草糖浆。', '/images/products/latte.jpg', 25.00, 28.00, 10.00, 80, 312, '杯', 1, 1, 1, 1),
(3, 6, '经典卡布奇诺', 'P003', '浓缩咖啡与绵密奶泡融合。', '/images/products/cappuccino.jpg', 23.00, 25.00, 9.00, 60, 189, '杯', 1, 0, 0, 1),
(4, 6, '摩卡咖啡', 'P004', '咖啡与巧克力风味融合。', '/images/products/mocha.jpg', 28.00, 30.00, 11.00, 50, 167, '杯', 1, 1, 0, 0),
(5, 9, '珍珠奶茶', 'P005', '经典黑糖珍珠奶茶。', '/images/products/bubble-tea.jpg', 15.00, 18.00, 6.00, 120, 423, '杯', 1, 1, 0, 1),
(6, 10, '芒果果茶', 'P006', '芒果与茉莉茶清爽搭配。', '/images/products/mango-tea.jpg', 22.00, 25.00, 8.00, 90, 278, '杯', 1, 1, 1, 0),
(7, 11, '提拉米苏', 'P007', '经典意式风味甜品。', '/images/products/tiramisu.jpg', 32.00, 35.00, 12.00, 40, 98, '份', 1, 0, 0, 1),
(8, 11, '蔓越莓曲奇', 'P008', '香脆可口，蔓越莓果香。', '/images/products/cookie.jpg', 18.00, 20.00, 7.00, 150, 134, '袋', 1, 0, 1, 0),
(9, 5, '焦糖玛奇朵', 'P009', '焦糖与浓缩咖啡香气融合。', '/images/products/caramel-macchiato.jpg', 26.00, 30.00, 10.50, 85, 145, '杯', 1, 1, 0, 1),
(11, 6, '燕麦拿铁', 'P011', '使用燕麦奶制作，更轻盈。', '/images/products/oat-latte.jpg', 28.00, 32.00, 12.00, 95, 67, '杯', 1, 0, 1, 1);

INSERT INTO `product_image` (`id`, `product_id`, `image_url`, `sort_order`, `create_time`) VALUES
(1101, 1, '/images/products/americano-01.jpg', 1, '2026-03-01 09:00:00'),
(1102, 2, '/images/products/latte-01.jpg', 1, '2026-03-01 09:00:00'),
(1103, 5, '/images/products/bubble-tea-01.jpg', 1, '2026-03-01 09:00:00'),
(1104, 11, '/images/products/oat-latte-01.jpg', 1, '2026-03-01 09:00:00');

INSERT INTO `product_spec` (`id`, `product_id`, `spec_name`, `spec_values`, `create_time`) VALUES
(1201, 1, '杯型', '["中杯","大杯"]', '2026-03-01 09:05:00'),
(1202, 2, '温度', '["热饮","冰饮"]', '2026-03-01 09:05:00');

INSERT INTO `product_sku` (`id`, `product_id`, `sku_code`, `sku_name`, `spec_info`, `price`, `stock`, `image`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(1301, 1, 'SKU-P001-M', '经典美式 中杯', '杯型:中杯', 18.00, 60, '/images/products/americano-01.jpg', 1, 0, '2026-03-01 09:10:00', '2026-03-29 08:00:00'),
(1302, 1, 'SKU-P001-L', '经典美式 大杯', '杯型:大杯', 22.00, 40, '/images/products/americano-01.jpg', 1, 0, '2026-03-01 09:10:00', '2026-03-29 08:00:00'),
(1303, 2, 'SKU-P002-H', '香草拿铁 热饮', '温度:热饮', 25.00, 42, '/images/products/latte-01.jpg', 1, 0, '2026-03-01 09:10:00', '2026-03-29 08:00:00'),
(1304, 2, 'SKU-P002-I', '香草拿铁 冰饮', '温度:冰饮', 26.00, 38, '/images/products/latte-01.jpg', 1, 0, '2026-03-01 09:10:00', '2026-03-29 08:00:00');

INSERT INTO `product_stock_log` (`id`, `product_id`, `sku_id`, `change_type`, `change_quantity`, `before_stock`, `after_stock`, `remark`, `operator_id`, `create_time`) VALUES
(1401, 1, 1301, 1, 20, 40, 60, '春季补货', 20, '2026-03-01 09:20:00'),
(1402, 2, 1303, 1, 22, 20, 42, '春季补货', 20, '2026-03-01 09:20:00'),
(1403, 2, 1303, 2, 2, 42, 40, '订单 ORD202603200103 扣减库存', 20, '2026-03-20 14:00:00'),
(1404, 2, 1303, 3, 1, 40, 41, '订单 ORD202603150104 退款回补库存', 21, '2026-03-15 17:00:00');

INSERT INTO `member_level` (`id`, `level_name`, `level_no`, `required_points`, `discount_rate`, `description`, `icon`, `status`) VALUES
(1, '普通会员', 1, 0, 1.00, '入门会员等级', 'icon-regular', 1),
(2, '青铜会员', 2, 100, 0.98, '积分满 100 升级', 'icon-bronze', 1),
(3, '白银会员', 3, 500, 0.95, '积分满 500 升级', 'icon-silver', 1),
(4, '黄金会员', 4, 1000, 0.92, '积分满 1000 升级', 'icon-gold', 1),
(5, '钻石会员', 5, 3000, 0.88, '积分满 3000 升级', 'icon-diamond', 1);

INSERT INTO `member_info` (`id`, `user_id`, `level_id`, `points`, `growth_value`, `total_consume`, `create_time`, `update_time`) VALUES
(1, 2, 4, 2000, 15000, 15000.00, '2026-03-01 08:15:00', '2026-03-29 09:00:00'),
(2, 3, 3, 680, 1250, 865.50, '2026-03-01 08:25:00', '2026-03-29 09:00:00'),
(3, 4, 2, 280, 380, 245.00, '2026-03-01 08:30:00', '2026-03-29 09:00:00'),
(4, 22, 5, 3650, 3620, 3268.00, '2026-03-02 09:00:00', '2026-03-29 09:00:00'),
(5, 23, 4, 1480, 1420, 1326.00, '2026-03-03 09:30:00', '2026-03-29 09:30:00'),
(6, 24, 3, 520, 520, 468.00, '2026-03-04 10:00:00', '2026-03-29 10:00:00'),
(7, 25, 2, 120, 120, 107.00, '2026-03-05 10:30:00', '2026-03-29 10:30:00');

INSERT INTO `member_benefit` (`id`, `benefit_name`, `benefit_code`, `benefit_type`, `benefit_value`, `description`, `icon`, `status`) VALUES
(1, '等级专属折扣', 'LEVEL_DISCOUNT', 1, 0.95, '结算时享受当前会员等级配置的专属折扣', 'icon-discount', 1),
(2, '积分加速', 'POINTS_ACCELERATOR', 2, 1.20, '下单后按权益倍率获得积分奖励', 'icon-points', 1),
(3, '免配送费', 'FREE_SHIPPING', 3, NULL, '满足等级条件后可享受配送费减免', 'icon-shipping', 1),
(4, '生日礼券', 'BIRTHDAY_COUPON', 4, 20.00, '生日月可获得专属礼券', 'icon-birthday', 1),
(5, '专属客服', 'EXCLUSIVE_SERVICE', 5, NULL, '高等级会员可享受优先客服支持', 'icon-service', 1);

INSERT INTO `member_level_benefit` (`id`, `level_id`, `benefit_id`, `sort_order`, `status`) VALUES
(1, 2, 1, 1, 1),
(2, 3, 1, 1, 1),
(3, 3, 2, 2, 1),
(4, 4, 1, 1, 1),
(5, 4, 2, 2, 1),
(6, 4, 4, 3, 1),
(7, 5, 1, 1, 1),
(8, 5, 2, 2, 1),
(9, 5, 3, 3, 1),
(10, 5, 4, 4, 1),
(11, 5, 5, 5, 1);

INSERT INTO `delivery_method` (`id`, `method_name`, `description`, `base_freight`, `free_freight_amount`) VALUES
(1, '标准配送', '3-5 个工作日送达', 8.00, 50.00),
(2, '快速配送', '1-2 个工作日送达', 15.00, 100.00),
(3, '门店自提', '2 小时可取', 0.00, 0.00);

INSERT INTO `delivery_region` (`id`, `parent_id`, `region_name`, `region_code`, `level`, `delivery_fee`, `min_order_amount`, `estimated_time`, `longitude`, `latitude`, `delivery_range`, `sort_order`) VALUES
(1, 0, '北京市', 'BJ', 1, 0.00, 0.00, 0, 116.397128, 39.916527, 0.0, 1),
(2, 0, '上海市', 'SH', 1, 0.00, 0.00, 0, 121.473701, 31.230416, 0.0, 2),
(3, 1, '朝阳区', 'BJCY', 2, 8.00, 30.00, 45, 116.407526, 39.904989, 15.0, 1),
(4, 1, '海淀区', 'BJHD', 2, 8.00, 30.00, 50, 116.298056, 39.959988, 18.0, 2);

INSERT INTO `delivery_staff` (`id`, `user_id`, `region_id`, `staff_code`, `name`, `phone`, `id_number`, `vehicle_type`, `vehicle_number`, `status`, `today_orders`, `total_orders`, `rating`, `join_time`, `health_cert_no`, `health_cert_expiry`, `enabled`) VALUES
(1, 6, 3, 'DS001', '张强', '13900002001', '110101199001011234', 'EBIKE', 'BJ-A12345', 'IDLE', 5, 328, 4.8, '2023-01-15 09:00:00', 'HC202301001', '2026-12-31', 1),
(2, 7, 4, 'DS002', '李雷', '13900002002', '110101199002021234', 'MOTORCYCLE', 'BJ-B67890', 'BUSY', 8, 256, 4.6, '2023-03-20 10:30:00', 'HC202303002', '2026-11-30', 1);

INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('site_name', '线上咖啡店', 'string', '站点名称'),
('customer_service_phone', '400-123-4567', 'string', '客服电话'),
('order_auto_cancel_time', '30', 'number', '订单自动取消分钟数'),
('register_points', '10', 'number', '注册赠送积分');

INSERT INTO `banner` (`id`, `title`, `image_url`, `link_url`, `sort_order`, `position`, `status`) VALUES
(1, '欢迎光临', '/images/banner/banner1.jpg', '/product', 1, 'home', 1),
(2, '限时优惠', '/images/banner/banner2.jpg', '/promotion', 2, 'home', 1);

INSERT INTO `user_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail_address`, `is_default`, `create_time`, `update_time`) VALUES
(101, 22, '陈晨', '13910002022', '北京市', '北京市', '朝阳区', '望京 SOHO T2-1208', 1, '2026-03-02 09:10:00', '2026-03-02 09:10:00'),
(102, 23, '林然', '13910002023', '北京市', '北京市', '海淀区', '软件园 3 号楼 502', 1, '2026-03-03 09:40:00', '2026-03-03 09:40:00'),
(103, 24, '乔安', '13910002024', '北京市', '北京市', '西城区', '金融街 8 号 B 座 1511', 1, '2026-03-04 10:10:00', '2026-03-04 10:10:00'),
(104, 25, '孙悦', '13910002025', '上海市', '上海市', '浦东新区', '张江科技路 88 号 7 楼', 1, '2026-03-05 10:40:00', '2026-03-05 10:40:00');

INSERT INTO `coupon` (`id`, `coupon_name`, `coupon_type`, `discount_type`, `discount_value`, `min_amount`, `total_quantity`, `received_quantity`, `start_time`, `end_time`, `description`, `status`) VALUES
(101, '早间立减 10 元', 1, 1, 10.00, 50.00, 500, 82, '2026-03-01 00:00:00', '2026-12-31 23:59:59', '满 50 元立减 10 元', 1),
(102, '拿铁周立减 15 元', 1, 1, 15.00, 80.00, 300, 41, '2026-03-20 00:00:00', '2026-04-30 23:59:59', '满 80 元立减 15 元', 1);

INSERT INTO `user_coupon` (`id`, `user_id`, `coupon_id`, `coupon_code`, `status`, `use_time`, `order_id`, `expire_time`, `receive_time`) VALUES
(901, 22, 101, 'UC20260322001', 0, NULL, NULL, '2026-12-31 23:59:59', '2026-03-22 10:00:00'),
(902, 22, 102, 'UC20260319002', 1, '2026-03-20 14:00:00', 103, '2026-04-30 23:59:59', '2026-03-19 09:00:00');

INSERT INTO `promotion_activity` (`id`, `activity_name`, `activity_type`, `start_time`, `end_time`, `description`, `banner`, `status`, `create_time`, `update_time`) VALUES
(101, '春日咖啡周', 1, '2026-03-20 00:00:00', '2026-04-10 23:59:59', '咖啡单品限时优惠', '/images/banner/spring-coffee-week.jpg', 1, '2026-03-18 09:00:00', '2026-03-18 09:00:00'),
(102, '下午茶特惠', 2, '2026-03-01 00:00:00', '2026-12-31 23:59:59', '饮品与甜品组合优惠', '/images/banner/tea-time.jpg', 1, '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO `promotion_product` (`id`, `activity_id`, `product_id`, `promotion_price`, `stock_limit`, `create_time`) VALUES
(1001, 101, 1, 16.00, 2, '2026-03-20 00:00:00'),
(1002, 101, 2, 22.00, 2, '2026-03-20 00:00:00');

INSERT INTO `shopping_cart` (`id`, `user_id`, `product_id`, `sku_id`, `quantity`, `checked`, `create_time`, `update_time`) VALUES
(501, 22, 11, NULL, 1, 1, '2026-03-29 08:10:00', '2026-03-29 08:10:00'),
(502, 23, 2, 1304, 2, 1, '2026-03-29 08:20:00', '2026-03-29 08:20:00');

INSERT INTO `orders` (`id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `discount_amount`, `freight_amount`, `order_status`, `pay_status`, `pay_type`, `status`, `payment_method`, `pay_time`, `receiver_name`, `receiver_phone`, `receiver_address`, `remark`, `create_time`, `update_time`) VALUES
(101, 'ORD202603280101', 22, 68.00, 68.00, 0.00, 0.00, 1, 0, NULL, 'PENDING_PAYMENT', NULL, NULL, '陈晨', '13910002022', '北京市朝阳区望京 SOHO T2-1208', '少冰', '2026-03-28 09:15:00', '2026-03-28 09:15:00'),
(102, 'ORD202603270102', 23, 84.00, 79.00, 5.00, 0.00, 3, 1, 4, 'SHIPPED', 'MOCK_PAY', '2026-03-27 10:20:00', '林然', '13910002023', '北京市海淀区软件园 3 号楼 502', '尽快送达', '2026-03-27 10:12:00', '2026-03-27 10:20:00'),
(103, 'ORD202603200103', 22, 91.00, 81.00, 10.00, 0.00, 4, 1, 1, 'COMPLETED', 'ALIPAY', '2026-03-20 14:02:00', '陈晨', '13910002022', '北京市朝阳区望京 SOHO T2-1208', '需要个人发票', '2026-03-20 13:48:00', '2026-03-21 10:00:00'),
(104, 'ORD202603150104', 24, 58.00, 53.00, 5.00, 0.00, 6, 1, 4, 'REFUNDING', 'MOCK_PAY', '2026-03-15 10:05:00', '乔安', '13910002024', '北京市西城区金融街 8 号 B 座 1511', '口味不符申请退款', '2026-03-15 09:50:00', '2026-03-16 11:30:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `sku_id`, `product_name`, `product_image`, `spec_info`, `price`, `quantity`, `total_amount`, `create_time`) VALUES
(1001, 101, 2, 1303, '香草拿铁', '/images/products/latte.jpg', '温度:热饮', 25.00, 2, 50.00, '2026-03-28 09:15:00'),
(1002, 101, 8, NULL, '蔓越莓曲奇', '/images/products/cookie.jpg', NULL, 18.00, 1, 18.00, '2026-03-28 09:15:00'),
(1003, 102, 1, 1301, '经典美式', '/images/products/americano.jpg', '杯型:中杯', 18.00, 2, 36.00, '2026-03-27 10:12:00'),
(1004, 102, 5, NULL, '珍珠奶茶', '/images/products/bubble-tea.jpg', NULL, 15.00, 2, 30.00, '2026-03-27 10:12:00'),
(1005, 103, 2, 1303, '香草拿铁', '/images/products/latte.jpg', '温度:热饮', 25.00, 2, 50.00, '2026-03-20 13:48:00'),
(1006, 103, 3, NULL, '经典卡布奇诺', '/images/products/cappuccino.jpg', NULL, 23.00, 1, 23.00, '2026-03-20 13:48:00'),
(1007, 104, 1, 1301, '经典美式', '/images/products/americano.jpg', '杯型:中杯', 18.00, 1, 18.00, '2026-03-15 09:50:00'),
(1008, 104, 5, NULL, '珍珠奶茶', '/images/products/bubble-tea.jpg', NULL, 15.00, 1, 15.00, '2026-03-15 09:50:00'),
(1009, 104, 2, 1303, '香草拿铁', '/images/products/latte.jpg', '温度:热饮', 25.00, 1, 25.00, '2026-03-15 09:50:00');

INSERT INTO `payment` (`id`, `payment_no`, `order_id`, `order_no`, `user_id`, `pay_amount`, `pay_type`, `pay_status`, `trade_no`, `pay_time`, `deleted`, `create_time`, `update_time`) VALUES
(201, 'PAY202603270201', 102, 'ORD202603270102', 23, 79.00, 4, 1, 'MOCK202603270201', '2026-03-27 10:20:00', 0, '2026-03-27 10:15:00', '2026-03-27 10:20:00'),
(202, 'PAY202603200202', 103, 'ORD202603200103', 22, 81.00, 1, 1, 'ALI202603200202', '2026-03-20 14:02:00', 0, '2026-03-20 13:55:00', '2026-03-20 14:02:00'),
(203, 'PAY202603150203', 104, 'ORD202603150104', 24, 53.00, 4, 1, 'MOCK202603150203', '2026-03-15 10:05:00', 0, '2026-03-15 10:00:00', '2026-03-16 11:30:00');

INSERT INTO `delivery_order` (`id`, `order_id`, `delivery_no`, `staff_id`, `delivery_status`, `assign_time`, `pickup_time`, `delivery_time`, `remark`, `create_time`, `update_time`) VALUES
(301, 102, 'DL202603270301', 1, 3, '2026-03-27 10:30:00', '2026-03-27 10:45:00', NULL, '配送中', '2026-03-27 10:20:00', '2026-03-27 10:45:00'),
(302, 103, 'DL202603200302', 2, 4, '2026-03-20 14:05:00', '2026-03-20 14:20:00', '2026-03-20 15:05:00', '已送达', '2026-03-20 14:02:00', '2026-03-20 15:05:00'),
(303, 104, 'DL202603150303', 1, 4, '2026-03-15 10:06:00', '2026-03-15 10:20:00', '2026-03-15 11:00:00', '已送达后发起退款', '2026-03-15 10:05:00', '2026-03-15 11:00:00');

INSERT INTO `delivery_track` (`id`, `delivery_order_id`, `track_info`, `longitude`, `latitude`, `create_time`) VALUES
(401, 301, '骑手已接单，前往门店', 116.407526, 39.904989, '2026-03-27 10:31:00'),
(402, 301, '餐品已出库，正在配送', 116.397128, 39.916527, '2026-03-27 10:45:00'),
(403, 302, '订单已送达并签收', 116.307000, 39.980000, '2026-03-20 15:05:00');

INSERT INTO `invoice` (`id`, `invoice_no`, `order_id`, `user_id`, `invoice_type`, `title_type`, `title`, `tax_no`, `amount`, `status`, `issue_time`, `create_time`) VALUES
(501, 'INV202603210501', 103, 22, 1, 1, '陈晨', NULL, 81.00, 2, '2026-03-21 10:00:00', '2026-03-20 14:30:00'),
(502, 'INV202603160502', 104, 24, 1, 1, '乔安', NULL, 53.00, 1, NULL, '2026-03-16 12:00:00');

INSERT INTO `refund` (`id`, `refund_no`, `order_id`, `order_no`, `user_id`, `refund_amount`, `refund_reason`, `refund_status`, `refund_time`, `deleted`, `create_time`, `update_time`) VALUES
(601, 'REF202603160601', 104, 'ORD202603150104', 24, 53.00, '口味不符，申请退款', 0, NULL, 0, '2026-03-16 11:00:00', '2026-03-16 11:30:00');

INSERT INTO `order_after_sale` (`id`, `order_id`, `user_id`, `after_sale_no`, `type`, `reason`, `description`, `images`, `refund_amount`, `status`, `handle_time`, `handle_remark`, `deleted`, `create_time`, `update_time`) VALUES
(701, 104, 24, 'AS202603160701', 1, '口味不符', '实际口感与预期差异较大', '[]', 53.00, 4, '2026-03-16 11:30:00', '退款处理中', 0, '2026-03-16 11:00:00', '2026-03-16 11:30:00');

INSERT INTO `product_review` (`id`, `product_id`, `user_id`, `order_id`, `rating`, `content`, `images`, `is_anonymous`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(101, 2, 22, 103, 5, '奶泡细腻，甜度平衡，整体口感很好。', '[]', 0, 1, 0, '2026-03-21 19:20:00', '2026-03-21 19:20:00');

INSERT INTO `review_reply` (`id`, `review_id`, `user_id`, `content`, `deleted`, `create_time`) VALUES
(101, 101, 20, '感谢反馈，欢迎下次体验我们的季节限定。', 0, '2026-03-22 09:00:00');

INSERT INTO `points_record` (`id`, `user_id`, `change_type`, `change_points`, `before_points`, `after_points`, `source_type`, `source_id`, `remark`, `create_time`) VALUES
(301, 22, 1, 81, 3569, 3650, 'ORDER', 103, '订单支付奖励积分', '2026-03-20 14:05:00'),
(302, 23, 1, 79, 1401, 1480, 'ORDER', 102, '订单支付奖励积分', '2026-03-27 10:25:00');

INSERT INTO `financial_record` (`id`, `record_no`, `user_id`, `record_type`, `amount`, `balance`, `business_type`, `business_id`, `remark`, `create_time`) VALUES
(2201, 'FR2026032001', 22, 1, 81.00, 81.00, 'PAYMENT', 202, '订单支付入账', '2026-03-20 14:02:00'),
(2202, 'FR2026032702', 23, 1, 79.00, 160.00, 'PAYMENT', 201, '订单支付入账', '2026-03-27 10:20:00'),
(2203, 'FR2026031603', 24, 1, 0.00, 160.00, 'REFUND_PENDING', 601, '退款申请处理中，暂未出账', '2026-03-16 11:30:00');

INSERT INTO `user_favorite` (`id`, `user_id`, `product_id`, `create_time`) VALUES
(2001, 22, 2, '2026-03-18 10:00:00'),
(2002, 23, 5, '2026-03-21 15:00:00');

INSERT INTO `user_browse_history` (`id`, `user_id`, `product_id`, `create_time`) VALUES
(2101, 22, 2, '2026-03-19 08:40:00'),
(2102, 23, 1, '2026-03-27 09:50:00'),
(2103, 24, 7, '2026-03-16 09:08:00');

INSERT INTO `user_behavior` (`user_id`, `session_id`, `action_type`, `target_type`, `target_id`, `action_data`, `page_url`, `referrer`, `device_type`, `os`, `browser`, `ip_address`, `location`, `duration`, `create_time`) VALUES
(22, 'sess_member_chen_01', 'LOGIN', 'USER', 22, '{"entry":"login"}', '/login', NULL, 'PC', 'Windows 11', 'Chrome', '10.10.0.22', '北京', 18, '2026-03-29 08:55:00'),
(23, 'sess_member_lin_01', 'ORDER', 'ORDER', 102, '{"source":"cart"}', '/order', '/cart', 'MOBILE', 'iOS', 'Safari', '10.10.0.23', '北京', 95, '2026-03-27 10:12:00'),
(24, 'sess_member_qiao_01', 'PAY', 'ORDER', 104, '{"paymentNo":"PAY202603150203"}', '/payment', '/order', 'PC', 'macOS', 'Chrome', '10.10.0.24', '北京', 42, '2026-03-15 10:05:00');

INSERT INTO `sales_statistics_daily` (`id`, `stat_date`, `order_count`, `order_amount`, `pay_count`, `pay_amount`, `refund_count`, `refund_amount`, `new_user_count`, `active_user_count`, `create_time`, `update_time`) VALUES
(1601, '2026-03-20', 1, 91.00, 1, 81.00, 0, 0.00, 0, 4, '2026-03-20 23:59:00', '2026-03-20 23:59:00'),
(1602, '2026-03-27', 1, 84.00, 1, 79.00, 0, 0.00, 0, 6, '2026-03-27 23:59:00', '2026-03-27 23:59:00'),
(1603, '2026-03-16', 1, 58.00, 1, 53.00, 1, 53.00, 0, 3, '2026-03-16 23:59:00', '2026-03-16 23:59:00');

INSERT INTO `product_sales_statistics` (`id`, `product_id`, `stat_date`, `sales_count`, `sales_amount`, `view_count`, `collect_count`, `cart_count`, `create_time`, `update_time`) VALUES
(1501, 2, '2026-03-20', 2, 50.00, 18, 4, 5, '2026-03-20 23:59:00', '2026-03-20 23:59:00'),
(1502, 1, '2026-03-27', 2, 36.00, 12, 2, 3, '2026-03-27 23:59:00', '2026-03-27 23:59:00'),
(1503, 5, '2026-03-16', 1, 15.00, 15, 3, 4, '2026-03-16 23:59:00', '2026-03-16 23:59:00');

INSERT INTO `user_active_statistics` (`id`, `user_id`, `stat_date`, `login_count`, `browse_count`, `order_count`, `pay_count`, `online_time`, `create_time`, `update_time`) VALUES
(1701, 22, '2026-03-20', 1, 6, 1, 1, 38, '2026-03-20 23:59:00', '2026-03-20 23:59:00'),
(1702, 23, '2026-03-27', 1, 7, 1, 1, 41, '2026-03-27 23:59:00', '2026-03-27 23:59:00'),
(1703, 24, '2026-03-16', 1, 5, 1, 1, 30, '2026-03-16 23:59:00', '2026-03-16 23:59:00');

INSERT INTO `operation_summary` (`id`, `stat_date`, `total_user`, `new_user`, `active_user`, `total_order`, `total_amount`, `avg_order_amount`, `conversion_rate`, `repurchase_rate`, `product_count`, `category_count`, `create_time`, `update_time`) VALUES
(1801, '2026-03-20', 12, 0, 4, 1, 91.00, 91.00, 0.3200, 0.2400, 10, 8, '2026-03-20 23:59:00', '2026-03-20 23:59:00'),
(1802, '2026-03-27', 12, 0, 6, 1, 84.00, 84.00, 0.3600, 0.2850, 10, 8, '2026-03-27 23:59:00', '2026-03-27 23:59:00'),
(1803, '2026-03-16', 12, 0, 3, 1, 58.00, 58.00, 0.2600, 0.1800, 10, 8, '2026-03-16 23:59:00', '2026-03-16 23:59:00');

INSERT INTO `notice` (`id`, `title`, `content`, `notice_type`, `publish_time`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(1901, '春日咖啡周上线', '活动商品与优惠券已开放领取。', 2, '2026-03-20 09:00:00', 1, 0, '2026-03-20 08:50:00', '2026-03-20 08:50:00');

INSERT INTO `user_coupon` (`id`, `user_id`, `coupon_id`, `coupon_code`, `status`, `use_time`, `order_id`, `expire_time`, `receive_time`) VALUES
(903, 23, 101, 'UC20260301003', 2, NULL, NULL, '2026-03-10 23:59:59', '2026-03-01 09:00:00'),
(904, 24, 102, 'UC20260305004', 3, NULL, NULL, '2026-04-30 23:59:59', '2026-03-05 11:00:00');

INSERT INTO `promotion_activity` (`id`, `activity_name`, `activity_type`, `start_time`, `end_time`, `description`, `banner`, `status`, `create_time`, `update_time`) VALUES
(103, '冬季清仓', 1, '2026-01-01 00:00:00', '2026-02-28 23:59:59', '用于筛选测试的已结束活动', '/images/banner/winter-clearance.jpg', 0, '2026-01-01 09:00:00', '2026-02-28 23:59:59'),
(104, '四月新品周', 1, '2026-04-01 00:00:00', '2026-04-07 23:59:59', '用于预热展示的待开始活动', '/images/banner/april-new-week.jpg', 2, '2026-03-30 09:00:00', '2026-03-30 09:00:00');

INSERT INTO `promotion_product` (`id`, `activity_id`, `product_id`, `promotion_price`, `stock_limit`, `create_time`) VALUES
(1003, 104, 6, 19.90, 2, '2026-03-30 09:00:00'),
(1004, 104, 11, 24.90, 2, '2026-03-30 09:00:00');

INSERT INTO `orders` (`id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `discount_amount`, `freight_amount`, `order_status`, `pay_status`, `pay_type`, `status`, `payment_method`, `pay_time`, `delivery_time`, `receive_time`, `cancel_time`, `cancel_reason`, `receiver_name`, `receiver_phone`, `receiver_address`, `remark`, `create_time`, `update_time`) VALUES
(105, 'ORD202603290105', 25, 79.00, 74.00, 5.00, 0.00, 2, 1, 2, 'PAID', 'WECHAT', '2026-03-29 12:10:00', NULL, NULL, NULL, NULL, '孙悦', '13910002025', '上海市浦东新区张江科技路 88 号 7 楼', '少糖', '2026-03-29 12:00:00', '2026-03-29 12:10:00'),
(106, 'ORD202603300106', 23, 54.00, 54.00, 0.00, 0.00, 3, 1, 1, 'SHIPPED', 'ALIPAY', '2026-03-30 09:20:00', '2026-03-30 09:30:00', NULL, NULL, NULL, '林然', '13910002023', '北京市海淀区软件园 3 号楼 502', '放前台即可', '2026-03-30 09:10:00', '2026-03-30 09:30:00'),
(107, 'ORD202603260107', 24, 46.00, 46.00, 0.00, 0.00, 5, 0, NULL, 'CANCELLED', NULL, NULL, NULL, NULL, '2026-03-26 18:45:00', '支付超时自动取消', '乔安', '13910002024', '北京市西城区金融街 8 号 B 座 1511', '超时请取消', '2026-03-26 18:10:00', '2026-03-26 18:45:00'),
(108, 'ORD202603240108', 23, 86.00, 81.00, 5.00, 0.00, 7, 2, 4, 'REFUNDED', 'MOCK_PAY', '2026-03-24 10:05:00', '2026-03-24 10:40:00', '2026-03-24 11:20:00', NULL, NULL, '林然', '13910002023', '北京市海淀区软件园 3 号楼 502', '商品质量问题已退款', '2026-03-24 09:50:00', '2026-03-25 14:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `sku_id`, `product_name`, `product_image`, `spec_info`, `price`, `quantity`, `total_amount`, `create_time`) VALUES
(1010, 105, 6, NULL, '芒果果茶', '/images/products/mango-tea.jpg', NULL, 22.00, 2, 44.00, '2026-03-29 12:00:00'),
(1011, 105, 8, NULL, '蔓越莓曲奇', '/images/products/cookie.jpg', NULL, 18.00, 1, 18.00, '2026-03-29 12:00:00'),
(1012, 106, 1, 1302, '经典美式', '/images/products/americano.jpg', '杯型:大杯', 22.00, 1, 22.00, '2026-03-30 09:10:00'),
(1013, 106, 9, NULL, '焦糖玛奇朵', '/images/products/caramel-macchiato.jpg', NULL, 26.00, 1, 26.00, '2026-03-30 09:10:00'),
(1014, 107, 3, NULL, '经典卡布奇诺', '/images/products/cappuccino.jpg', NULL, 23.00, 2, 46.00, '2026-03-26 18:10:00'),
(1015, 108, 11, NULL, '燕麦拿铁', '/images/products/oat-latte.jpg', NULL, 28.00, 2, 56.00, '2026-03-24 09:50:00'),
(1016, 108, 5, NULL, '珍珠奶茶', '/images/products/bubble-tea.jpg', NULL, 15.00, 2, 30.00, '2026-03-24 09:50:00');

INSERT INTO `payment` (`id`, `payment_no`, `order_id`, `order_no`, `user_id`, `pay_amount`, `pay_type`, `pay_status`, `trade_no`, `pay_time`, `deleted`, `create_time`, `update_time`) VALUES
(204, 'PAY202603280204', 101, 'ORD202603280101', 22, 68.00, 4, 0, NULL, NULL, 0, '2026-03-28 09:16:00', '2026-03-28 09:16:00'),
(205, 'PAY202603260205', 107, 'ORD202603260107', 24, 46.00, 2, 3, 'WX_CLOSE_202603260205', NULL, 0, '2026-03-26 18:15:00', '2026-03-26 18:45:00'),
(206, 'PAY202603240206', 108, 'ORD202603240108', 23, 81.00, 4, 1, 'MOCK202603240206', '2026-03-24 10:05:00', 0, '2026-03-24 10:00:00', '2026-03-25 14:00:00'),
(207, 'PAY202603290207', 105, 'ORD202603290105', 25, 74.00, 2, 1, 'WX202603290207', '2026-03-29 12:10:00', 0, '2026-03-29 12:05:00', '2026-03-29 12:10:00'),
(208, 'PAY202603300208', 106, 'ORD202603300106', 23, 54.00, 1, 1, 'ALI202603300208', '2026-03-30 09:20:00', 0, '2026-03-30 09:15:00', '2026-03-30 09:20:00'),
(209, 'PAY202603280209', 101, 'ORD202603280101', 22, 68.00, 1, 2, 'ALI_FAIL_202603280209', NULL, 0, '2026-03-28 09:18:00', '2026-03-28 09:19:00');

INSERT INTO `delivery_order` (`id`, `order_id`, `delivery_no`, `staff_id`, `delivery_status`, `assign_time`, `pickup_time`, `delivery_time`, `remark`, `create_time`, `update_time`) VALUES
(304, 105, 'DL202603290304', 2, 1, '2026-03-29 12:20:00', NULL, NULL, '已分配骑手，待取货', '2026-03-29 12:10:00', '2026-03-29 12:20:00'),
(305, 106, 'DL202603300305', 1, 2, '2026-03-30 09:25:00', '2026-03-30 09:30:00', NULL, '已取货，配送途中', '2026-03-30 09:20:00', '2026-03-30 09:30:00'),
(306, 108, 'DL202603240306', 2, 4, '2026-03-24 10:10:00', '2026-03-24 10:20:00', '2026-03-24 10:40:00', '先签收后完成退款', '2026-03-24 10:05:00', '2026-03-24 10:40:00');

INSERT INTO `delivery_track` (`id`, `delivery_order_id`, `track_info`, `longitude`, `latitude`, `create_time`) VALUES
(404, 304, '订单已分配骑手，等待商家出餐', 121.473701, 31.230416, '2026-03-29 12:21:00'),
(405, 305, '骑手已取货，离店配送中', 116.397128, 39.916527, '2026-03-30 09:31:00'),
(406, 306, '订单已送达并签收', 116.298056, 39.959988, '2026-03-24 10:40:00');

INSERT INTO `invoice` (`id`, `invoice_no`, `order_id`, `user_id`, `invoice_type`, `title_type`, `title`, `tax_no`, `amount`, `status`, `issue_time`, `create_time`) VALUES
(503, 'INV202603250503', 108, 23, 1, 2, '北京林然科技有限公司', '91110108LIN00001X', 81.00, 3, '2026-03-25 15:00:00', '2026-03-25 14:30:00'),
(504, 'INV202603300504', 106, 23, 1, 1, '林然', NULL, 54.00, 1, NULL, '2026-03-30 10:00:00');

INSERT INTO `refund` (`id`, `refund_no`, `order_id`, `order_no`, `user_id`, `refund_amount`, `refund_reason`, `refund_status`, `refund_time`, `deleted`, `create_time`, `update_time`) VALUES
(602, 'REF202603250602', 108, 'ORD202603240108', 23, 81.00, '奶泡口感异常，申请退款', 1, '2026-03-25 14:00:00', 0, '2026-03-25 11:00:00', '2026-03-25 14:00:00'),
(603, 'REF202603300603', 106, 'ORD202603300106', 23, 54.00, '配送已取货后申请退款', 2, NULL, 0, '2026-03-30 11:00:00', '2026-03-30 12:00:00');

INSERT INTO `order_after_sale` (`id`, `order_id`, `user_id`, `after_sale_no`, `type`, `reason`, `description`, `images`, `refund_amount`, `status`, `handle_time`, `handle_remark`, `deleted`, `create_time`, `update_time`) VALUES
(702, 105, 25, 'AS202603290702', 2, '调整配送时间', '用户希望改为晚间配送', '[]', 0.00, 1, NULL, NULL, 0, '2026-03-29 12:30:00', '2026-03-29 12:30:00'),
(703, 108, 23, 'AS202603250703', 1, '质量问题', '奶泡状态不稳定，口感与预期不符', '[]', 81.00, 2, '2026-03-25 12:00:00', '已同意并完成退款', 0, '2026-03-25 11:00:00', '2026-03-25 14:00:00'),
(704, 106, 23, 'AS202603300704', 1, '临时不需要', '订单已在配送中，按规则驳回', '[]', 54.00, 3, '2026-03-30 12:00:00', '已驳回：骑手已取货', 0, '2026-03-30 11:00:00', '2026-03-30 12:00:00');

INSERT INTO `product_stock_log` (`id`, `product_id`, `sku_id`, `change_type`, `change_quantity`, `before_stock`, `after_stock`, `remark`, `operator_id`, `create_time`) VALUES
(1405, 6, NULL, 2, 2, 90, 88, '订单 ORD202603290105 扣减库存', 20, '2026-03-29 12:00:00'),
(1406, 8, NULL, 2, 1, 150, 149, '订单 ORD202603290105 扣减库存', 20, '2026-03-29 12:00:00'),
(1407, 3, NULL, 3, 2, 58, 60, '订单 ORD202603260107 取消回补库存', 21, '2026-03-26 18:46:00'),
(1408, 11, NULL, 3, 2, 93, 95, '订单 ORD202603240108 退款回补库存', 21, '2026-03-25 14:05:00');

INSERT INTO `points_record` (`id`, `user_id`, `change_type`, `change_points`, `before_points`, `after_points`, `source_type`, `source_id`, `remark`, `create_time`) VALUES
(303, 25, 1, 74, 120, 194, 'ORDER', 105, '订单支付奖励积分', '2026-03-29 12:11:00'),
(304, 23, 2, -81, 1480, 1399, 'REFUND', 108, '退款扣回积分', '2026-03-25 14:01:00');

INSERT INTO `financial_record` (`id`, `record_no`, `user_id`, `record_type`, `amount`, `balance`, `business_type`, `business_id`, `remark`, `create_time`) VALUES
(2204, 'FR2026032904', 25, 1, 74.00, 234.00, 'PAYMENT', 207, '订单支付入账', '2026-03-29 12:10:00'),
(2205, 'FR2026032505', 23, 2, -81.00, 153.00, 'REFUND', 602, '订单 108 退款出账', '2026-03-25 14:00:00'),
(2206, 'FR2026032606', 24, 3, 0.00, 153.00, 'CANCEL', 107, '订单支付前取消，无资金结算', '2026-03-26 18:45:00');

INSERT INTO `sales_statistics_daily` (`id`, `stat_date`, `order_count`, `order_amount`, `pay_count`, `pay_amount`, `refund_count`, `refund_amount`, `new_user_count`, `active_user_count`, `create_time`, `update_time`) VALUES
(1604, '2026-03-01', 2, 49.70, 1, 49.70, 0, 0.00, 0, 4, '2026-03-01 23:59:00', '2026-03-01 23:59:00'),
(1605, '2026-03-02', 3, 51.40, 1, 51.40, 0, 0.00, 1, 5, '2026-03-02 23:59:00', '2026-03-02 23:59:00'),
(1606, '2026-03-03', 1, 53.10, 1, 53.10, 0, 0.00, 0, 6, '2026-03-03 23:59:00', '2026-03-03 23:59:00'),
(1607, '2026-03-04', 2, 54.80, 1, 49.80, 0, 0.00, 0, 7, '2026-03-04 23:59:00', '2026-03-04 23:59:00'),
(1608, '2026-03-05', 3, 56.50, 0, 0.00, 0, 0.00, 1, 3, '2026-03-05 23:59:00', '2026-03-05 23:59:00'),
(1609, '2026-03-06', 1, 58.20, 1, 58.20, 0, 0.00, 0, 4, '2026-03-06 23:59:00', '2026-03-06 23:59:00'),
(1610, '2026-03-07', 2, 59.90, 1, 59.90, 0, 0.00, 0, 5, '2026-03-07 23:59:00', '2026-03-07 23:59:00'),
(1611, '2026-03-08', 3, 61.60, 1, 56.60, 0, 0.00, 0, 6, '2026-03-08 23:59:00', '2026-03-08 23:59:00'),
(1612, '2026-03-09', 1, 63.30, 1, 63.30, 0, 0.00, 1, 7, '2026-03-09 23:59:00', '2026-03-09 23:59:00'),
(1613, '2026-03-10', 2, 65.00, 0, 0.00, 0, 0.00, 0, 3, '2026-03-10 23:59:00', '2026-03-10 23:59:00'),
(1614, '2026-03-11', 3, 66.70, 1, 66.70, 0, 0.00, 0, 4, '2026-03-11 23:59:00', '2026-03-11 23:59:00'),
(1615, '2026-03-12', 1, 68.40, 1, 63.40, 1, 35.60, 0, 5, '2026-03-12 23:59:00', '2026-03-12 23:59:00'),
(1616, '2026-03-13', 2, 70.10, 1, 70.10, 0, 0.00, 0, 6, '2026-03-13 23:59:00', '2026-03-13 23:59:00'),
(1617, '2026-03-14', 3, 71.80, 1, 71.80, 0, 0.00, 1, 7, '2026-03-14 23:59:00', '2026-03-14 23:59:00'),
(1618, '2026-03-15', 1, 73.50, 0, 0.00, 0, 0.00, 0, 3, '2026-03-15 23:59:00', '2026-03-15 23:59:00'),
(1619, '2026-03-17', 3, 76.90, 1, 76.90, 0, 0.00, 0, 5, '2026-03-17 23:59:00', '2026-03-17 23:59:00'),
(1620, '2026-03-18', 1, 78.60, 1, 78.60, 0, 0.00, 0, 6, '2026-03-18 23:59:00', '2026-03-18 23:59:00'),
(1621, '2026-03-19', 2, 80.30, 1, 80.30, 0, 0.00, 0, 7, '2026-03-19 23:59:00', '2026-03-19 23:59:00'),
(1622, '2026-03-21', 1, 83.70, 1, 83.70, 0, 0.00, 0, 4, '2026-03-21 23:59:00', '2026-03-21 23:59:00'),
(1623, '2026-03-22', 2, 85.40, 1, 85.40, 0, 0.00, 1, 5, '2026-03-22 23:59:00', '2026-03-22 23:59:00'),
(1624, '2026-03-23', 3, 87.10, 1, 87.10, 0, 0.00, 0, 6, '2026-03-23 23:59:00', '2026-03-23 23:59:00'),
(1625, '2026-03-24', 1, 88.80, 1, 83.80, 1, 51.20, 0, 7, '2026-03-24 23:59:00', '2026-03-24 23:59:00'),
(1626, '2026-03-25', 2, 90.50, 0, 0.00, 1, 52.50, 0, 3, '2026-03-25 23:59:00', '2026-03-25 23:59:00'),
(1627, '2026-03-26', 3, 92.20, 1, 92.20, 0, 0.00, 0, 4, '2026-03-26 23:59:00', '2026-03-26 23:59:00'),
(1628, '2026-03-28', 2, 95.60, 1, 90.60, 0, 0.00, 0, 6, '2026-03-28 23:59:00', '2026-03-28 23:59:00'),
(1629, '2026-03-29', 3, 97.30, 1, 97.30, 0, 0.00, 0, 7, '2026-03-29 23:59:00', '2026-03-29 23:59:00'),
(1630, '2026-03-30', 1, 99.00, 0, 0.00, 1, 59.00, 0, 3, '2026-03-30 23:59:00', '2026-03-30 23:59:00');

INSERT INTO `product_sales_statistics` (`id`, `product_id`, `stat_date`, `sales_count`, `sales_amount`, `view_count`, `collect_count`, `cart_count`, `create_time`, `update_time`) VALUES
(1504, 2, '2026-03-01', 2, 50.00, 11, 2, 3, '2026-03-01 23:59:00', '2026-03-01 23:59:00'),
(1505, 2, '2026-03-02', 1, 25.00, 12, 3, 4, '2026-03-02 23:59:00', '2026-03-02 23:59:00'),
(1506, 2, '2026-03-03', 2, 50.00, 13, 4, 5, '2026-03-03 23:59:00', '2026-03-03 23:59:00'),
(1507, 2, '2026-03-04', 1, 25.00, 14, 1, 6, '2026-03-04 23:59:00', '2026-03-04 23:59:00'),
(1508, 2, '2026-03-05', 2, 50.00, 15, 2, 2, '2026-03-05 23:59:00', '2026-03-05 23:59:00'),
(1509, 2, '2026-03-06', 1, 25.00, 16, 3, 3, '2026-03-06 23:59:00', '2026-03-06 23:59:00'),
(1510, 2, '2026-03-07', 2, 50.00, 17, 4, 4, '2026-03-07 23:59:00', '2026-03-07 23:59:00'),
(1511, 2, '2026-03-08', 1, 25.00, 18, 1, 5, '2026-03-08 23:59:00', '2026-03-08 23:59:00'),
(1512, 2, '2026-03-09', 2, 50.00, 19, 2, 6, '2026-03-09 23:59:00', '2026-03-09 23:59:00'),
(1513, 2, '2026-03-10', 1, 25.00, 20, 3, 2, '2026-03-10 23:59:00', '2026-03-10 23:59:00'),
(1514, 2, '2026-03-11', 2, 50.00, 21, 4, 3, '2026-03-11 23:59:00', '2026-03-11 23:59:00'),
(1515, 2, '2026-03-12', 1, 25.00, 22, 1, 4, '2026-03-12 23:59:00', '2026-03-12 23:59:00'),
(1516, 2, '2026-03-13', 2, 50.00, 23, 2, 5, '2026-03-13 23:59:00', '2026-03-13 23:59:00'),
(1517, 2, '2026-03-14', 1, 25.00, 24, 3, 6, '2026-03-14 23:59:00', '2026-03-14 23:59:00'),
(1518, 2, '2026-03-15', 2, 50.00, 25, 4, 2, '2026-03-15 23:59:00', '2026-03-15 23:59:00'),
(1519, 2, '2026-03-17', 2, 50.00, 27, 2, 4, '2026-03-17 23:59:00', '2026-03-17 23:59:00'),
(1520, 2, '2026-03-18', 1, 25.00, 28, 3, 5, '2026-03-18 23:59:00', '2026-03-18 23:59:00'),
(1521, 2, '2026-03-19', 2, 50.00, 29, 4, 6, '2026-03-19 23:59:00', '2026-03-19 23:59:00'),
(1522, 2, '2026-03-21', 2, 50.00, 31, 2, 3, '2026-03-21 23:59:00', '2026-03-21 23:59:00'),
(1523, 2, '2026-03-22', 1, 25.00, 32, 3, 4, '2026-03-22 23:59:00', '2026-03-22 23:59:00'),
(1524, 2, '2026-03-23', 2, 50.00, 33, 4, 5, '2026-03-23 23:59:00', '2026-03-23 23:59:00'),
(1525, 2, '2026-03-24', 1, 25.00, 34, 1, 6, '2026-03-24 23:59:00', '2026-03-24 23:59:00'),
(1526, 2, '2026-03-25', 2, 50.00, 35, 2, 2, '2026-03-25 23:59:00', '2026-03-25 23:59:00'),
(1527, 2, '2026-03-26', 1, 25.00, 36, 3, 3, '2026-03-26 23:59:00', '2026-03-26 23:59:00'),
(1528, 2, '2026-03-28', 1, 25.00, 38, 1, 5, '2026-03-28 23:59:00', '2026-03-28 23:59:00'),
(1529, 2, '2026-03-29', 2, 50.00, 39, 2, 6, '2026-03-29 23:59:00', '2026-03-29 23:59:00'),
(1530, 2, '2026-03-30', 1, 25.00, 40, 3, 2, '2026-03-30 23:59:00', '2026-03-30 23:59:00');

INSERT INTO `user_active_statistics` (`id`, `user_id`, `stat_date`, `login_count`, `browse_count`, `order_count`, `pay_count`, `online_time`, `create_time`, `update_time`) VALUES
(1704, 22, '2026-03-01', 1, 5, 0, 0, 26, '2026-03-01 23:59:00', '2026-03-01 23:59:00'),
(1705, 22, '2026-03-02', 1, 6, 0, 0, 27, '2026-03-02 23:59:00', '2026-03-02 23:59:00'),
(1706, 22, '2026-03-03', 1, 7, 1, 0, 28, '2026-03-03 23:59:00', '2026-03-03 23:59:00'),
(1707, 22, '2026-03-04', 1, 4, 0, 1, 29, '2026-03-04 23:59:00', '2026-03-04 23:59:00'),
(1708, 22, '2026-03-05', 1, 5, 0, 0, 30, '2026-03-05 23:59:00', '2026-03-05 23:59:00'),
(1709, 22, '2026-03-06', 1, 6, 1, 0, 31, '2026-03-06 23:59:00', '2026-03-06 23:59:00'),
(1710, 22, '2026-03-07', 1, 7, 0, 0, 32, '2026-03-07 23:59:00', '2026-03-07 23:59:00'),
(1711, 22, '2026-03-08', 1, 4, 0, 1, 33, '2026-03-08 23:59:00', '2026-03-08 23:59:00'),
(1712, 22, '2026-03-09', 1, 5, 1, 0, 34, '2026-03-09 23:59:00', '2026-03-09 23:59:00'),
(1713, 22, '2026-03-10', 1, 6, 0, 0, 35, '2026-03-10 23:59:00', '2026-03-10 23:59:00'),
(1714, 22, '2026-03-11', 1, 7, 0, 0, 36, '2026-03-11 23:59:00', '2026-03-11 23:59:00'),
(1715, 22, '2026-03-12', 1, 4, 1, 1, 37, '2026-03-12 23:59:00', '2026-03-12 23:59:00'),
(1716, 22, '2026-03-13', 1, 5, 0, 0, 38, '2026-03-13 23:59:00', '2026-03-13 23:59:00'),
(1717, 22, '2026-03-14', 1, 6, 0, 0, 39, '2026-03-14 23:59:00', '2026-03-14 23:59:00'),
(1718, 22, '2026-03-15', 1, 7, 1, 0, 40, '2026-03-15 23:59:00', '2026-03-15 23:59:00'),
(1719, 22, '2026-03-17', 1, 5, 0, 0, 42, '2026-03-17 23:59:00', '2026-03-17 23:59:00'),
(1720, 22, '2026-03-18', 1, 6, 1, 0, 43, '2026-03-18 23:59:00', '2026-03-18 23:59:00'),
(1721, 22, '2026-03-19', 1, 7, 0, 0, 44, '2026-03-19 23:59:00', '2026-03-19 23:59:00'),
(1722, 22, '2026-03-21', 1, 5, 1, 0, 46, '2026-03-21 23:59:00', '2026-03-21 23:59:00'),
(1723, 22, '2026-03-22', 1, 6, 0, 0, 47, '2026-03-22 23:59:00', '2026-03-22 23:59:00'),
(1724, 22, '2026-03-23', 1, 7, 0, 0, 48, '2026-03-23 23:59:00', '2026-03-23 23:59:00'),
(1725, 22, '2026-03-24', 1, 4, 1, 1, 49, '2026-03-24 23:59:00', '2026-03-24 23:59:00'),
(1726, 22, '2026-03-25', 1, 5, 0, 0, 50, '2026-03-25 23:59:00', '2026-03-25 23:59:00'),
(1727, 22, '2026-03-26', 1, 6, 0, 0, 51, '2026-03-26 23:59:00', '2026-03-26 23:59:00'),
(1728, 22, '2026-03-28', 1, 4, 0, 1, 53, '2026-03-28 23:59:00', '2026-03-28 23:59:00'),
(1729, 22, '2026-03-29', 1, 5, 0, 0, 54, '2026-03-29 23:59:00', '2026-03-29 23:59:00'),
(1730, 22, '2026-03-30', 1, 6, 1, 0, 55, '2026-03-30 23:59:00', '2026-03-30 23:59:00');

INSERT INTO `operation_summary` (`id`, `stat_date`, `total_user`, `new_user`, `active_user`, `total_order`, `total_amount`, `avg_order_amount`, `conversion_rate`, `repurchase_rate`, `product_count`, `category_count`, `create_time`, `update_time`) VALUES
(1804, '2026-03-01', 12, 0, 4, 2, 49.70, 24.85, 0.2400, 0.1650, 10, 8, '2026-03-01 23:59:00', '2026-03-01 23:59:00'),
(1805, '2026-03-02', 12, 1, 5, 3, 51.40, 17.13, 0.2600, 0.1800, 10, 8, '2026-03-02 23:59:00', '2026-03-02 23:59:00'),
(1806, '2026-03-03', 12, 0, 6, 1, 53.10, 53.10, 0.2800, 0.1950, 10, 8, '2026-03-03 23:59:00', '2026-03-03 23:59:00'),
(1807, '2026-03-04', 12, 0, 7, 2, 54.80, 27.40, 0.3000, 0.2100, 10, 8, '2026-03-04 23:59:00', '2026-03-04 23:59:00'),
(1808, '2026-03-05', 12, 1, 3, 3, 56.50, 18.83, 0.3200, 0.1500, 10, 8, '2026-03-05 23:59:00', '2026-03-05 23:59:00'),
(1809, '2026-03-06', 12, 0, 4, 1, 58.20, 58.20, 0.2200, 0.1650, 10, 8, '2026-03-06 23:59:00', '2026-03-06 23:59:00'),
(1810, '2026-03-07', 12, 0, 5, 2, 59.90, 29.95, 0.2400, 0.1800, 10, 8, '2026-03-07 23:59:00', '2026-03-07 23:59:00'),
(1811, '2026-03-08', 12, 0, 6, 3, 61.60, 20.53, 0.2600, 0.1950, 10, 8, '2026-03-08 23:59:00', '2026-03-08 23:59:00'),
(1812, '2026-03-09', 12, 1, 7, 1, 63.30, 63.30, 0.2800, 0.2100, 10, 8, '2026-03-09 23:59:00', '2026-03-09 23:59:00'),
(1813, '2026-03-10', 12, 0, 3, 2, 65.00, 32.50, 0.3000, 0.1500, 10, 8, '2026-03-10 23:59:00', '2026-03-10 23:59:00'),
(1814, '2026-03-11', 12, 0, 4, 3, 66.70, 22.23, 0.3200, 0.1650, 10, 8, '2026-03-11 23:59:00', '2026-03-11 23:59:00'),
(1815, '2026-03-12', 12, 0, 5, 1, 68.40, 68.40, 0.2200, 0.1800, 10, 8, '2026-03-12 23:59:00', '2026-03-12 23:59:00'),
(1816, '2026-03-13', 12, 0, 6, 2, 70.10, 35.05, 0.2400, 0.1950, 10, 8, '2026-03-13 23:59:00', '2026-03-13 23:59:00'),
(1817, '2026-03-14', 12, 1, 7, 3, 71.80, 23.93, 0.2600, 0.2100, 10, 8, '2026-03-14 23:59:00', '2026-03-14 23:59:00'),
(1818, '2026-03-15', 12, 0, 3, 1, 73.50, 73.50, 0.2800, 0.1500, 10, 8, '2026-03-15 23:59:00', '2026-03-15 23:59:00'),
(1819, '2026-03-17', 12, 0, 5, 3, 76.90, 25.63, 0.3200, 0.1800, 10, 8, '2026-03-17 23:59:00', '2026-03-17 23:59:00'),
(1820, '2026-03-18', 12, 0, 6, 1, 78.60, 78.60, 0.2200, 0.1950, 10, 8, '2026-03-18 23:59:00', '2026-03-18 23:59:00'),
(1821, '2026-03-19', 12, 0, 7, 2, 80.30, 40.15, 0.2400, 0.2100, 10, 8, '2026-03-19 23:59:00', '2026-03-19 23:59:00'),
(1822, '2026-03-21', 12, 0, 4, 1, 83.70, 83.70, 0.2800, 0.1650, 10, 8, '2026-03-21 23:59:00', '2026-03-21 23:59:00'),
(1823, '2026-03-22', 13, 1, 5, 2, 85.40, 42.70, 0.3000, 0.1800, 10, 8, '2026-03-22 23:59:00', '2026-03-22 23:59:00'),
(1824, '2026-03-23', 13, 0, 6, 3, 87.10, 29.03, 0.3200, 0.1950, 10, 8, '2026-03-23 23:59:00', '2026-03-23 23:59:00'),
(1825, '2026-03-24', 13, 0, 7, 1, 88.80, 88.80, 0.2200, 0.2100, 10, 8, '2026-03-24 23:59:00', '2026-03-24 23:59:00'),
(1826, '2026-03-25', 13, 0, 3, 2, 90.50, 45.25, 0.2400, 0.1500, 10, 8, '2026-03-25 23:59:00', '2026-03-25 23:59:00'),
(1827, '2026-03-26', 13, 0, 4, 3, 92.20, 30.73, 0.2600, 0.1650, 10, 8, '2026-03-26 23:59:00', '2026-03-26 23:59:00'),
(1828, '2026-03-28', 13, 0, 6, 2, 95.60, 47.80, 0.3000, 0.1950, 10, 8, '2026-03-28 23:59:00', '2026-03-28 23:59:00'),
(1829, '2026-03-29', 13, 0, 7, 3, 97.30, 32.43, 0.3200, 0.2100, 10, 8, '2026-03-29 23:59:00', '2026-03-29 23:59:00'),
(1830, '2026-03-30', 13, 0, 3, 1, 99.00, 99.00, 0.2200, 0.1500, 10, 8, '2026-03-30 23:59:00', '2026-03-30 23:59:00');

SET FOREIGN_KEY_CHECKS = 1;

SELECT '咖啡店数据库初始化完成。' AS message;
SELECT CONCAT('数据库: coffee_shop') AS info;
SELECT '首次启动后，请先在初始化页面为测试账号设置统一密码。' AS password_setup_hint;
SELECT CONCAT('管理员账号: admin') AS admin_account;
SELECT CONCAT('测试账号: test') AS test_account;
SELECT CONCAT('用户账号: customer1, customer2') AS customer_accounts;
SELECT CONCAT('扩展演示账号: ops_lead, finance_desk, member_chen, member_lin, member_qiao, member_sun') AS extended_accounts;
