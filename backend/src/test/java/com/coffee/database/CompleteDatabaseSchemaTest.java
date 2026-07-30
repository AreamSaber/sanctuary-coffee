package com.coffee.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteDatabaseSchemaTest {

    @Test
    void completeImportSqlContainsDeliveryExceptionSchemaUsedByCode() throws IOException {
        String sql = readCompleteImportSql();

        assertTrue(sql.contains("`has_exception` TINYINT DEFAULT 0"));
        assertTrue(sql.contains("CREATE TABLE `delivery_exception`"));
        assertTrue(sql.contains("`handle_status` TINYINT DEFAULT 0"));
    }

    @Test
    void completeImportSqlContainsRefundReviewSchemaUsedByCode() throws IOException {
        String sql = readCompleteImportSql();

        assertTrue(sql.contains("CREATE TABLE `refund`"));
        assertTrue(sql.contains("`reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID'"));
        assertTrue(sql.contains("`review_time` DATETIME DEFAULT NULL COMMENT '审核时间'"));
        assertTrue(sql.contains("`review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注'"));
        assertTrue(sql.contains("`trade_no` VARCHAR(100) DEFAULT NULL"));
        assertTrue(sql.contains("CREATE TABLE `order_after_sale`"));
    }

    @Test
    void completeImportSqlContainsBenefitGrantLogSchemaUsedByCode() throws IOException {
        String sql = readCompleteImportSql();

        assertTrue(sql.contains("CREATE TABLE `benefit_grant_log`"));
        assertTrue(sql.contains("`benefit_type` TINYINT NOT NULL COMMENT '权益类型(冗余)'"));
        assertTrue(sql.contains("`grant_time` DATETIME NOT NULL COMMENT '发放时间'"));
    }

    private String readCompleteImportSql() throws IOException {
        return Files.readString(
                Path.of("..", "docs", "database", "coffee_shop_complete.sql").normalize(),
                StandardCharsets.UTF_8
        );
    }
}
