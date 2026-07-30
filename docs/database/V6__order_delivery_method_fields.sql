-- 为订单补充配送方式字段，使下单时选择的配送方式和运费可落库展示。
ALTER TABLE `orders`
  ADD COLUMN `delivery_method_id` BIGINT DEFAULT NULL AFTER `freight_amount`,
  ADD COLUMN `delivery_method_name` VARCHAR(50) DEFAULT NULL AFTER `delivery_method_id`;
