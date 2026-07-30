package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.entity.Invoice;
import com.coffee.vo.FinancialReportVO;

import java.time.LocalDate;
import java.util.Map;

public interface FinancialService {

    FinancialReportVO generateFinancialReport(LocalDate startDate, LocalDate endDate);

    FinancialReportVO getDailyReport(LocalDate date);

    FinancialReportVO getMonthlyReport(Integer year, Integer month);

    FinancialReportVO getYearlyReport(Integer year);

    void applyInvoice(Long userId, Long orderId, Map<String, Object> invoiceData);

    IPage<Invoice> getInvoicePage(Long userId, Integer pageNum, Integer pageSize, String status);

    Invoice getInvoiceDetail(Long userId, boolean admin, Long invoiceId);

    void issueInvoice(Long invoiceId);

    void resendInvoice(Long userId, boolean admin, Long invoiceId);

    byte[] exportFinancialReport(LocalDate startDate, LocalDate endDate, String format);
}
