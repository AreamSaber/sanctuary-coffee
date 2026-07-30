-- V7: 添加 locked_stock 库存预占字段
ALTER TABLE product ADD COLUMN locked_stock INT DEFAULT 0 COMMENT '锁定库存（已下单未支付）';
ALTER TABLE product_sku ADD COLUMN locked_stock INT DEFAULT 0 COMMENT '锁定库存（已下单未支付）';
