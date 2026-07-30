-- ============================================================
-- 退款审核字段补充（任务2：退款审核增强）
-- ============================================================

ALTER TABLE `refund`
  ADD COLUMN `reviewer_id`   BIGINT       DEFAULT NULL COMMENT '审核人ID' AFTER `refund_reason`,
  ADD COLUMN `review_time`   DATETIME     DEFAULT NULL COMMENT '审核时间' AFTER `reviewer_id`,
  ADD COLUMN `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注' AFTER `review_time`;

ALTER TABLE `order_after_sale`
  ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID' AFTER `handle_remark`;
