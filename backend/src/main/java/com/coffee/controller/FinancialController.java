package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.entity.Invoice;
import com.coffee.service.FinancialService;
import com.coffee.vo.FinancialReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Financial Management", description = "Financial report and invoice APIs")
@RestController
@RequestMapping("/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @Operation(summary = "Generate financial report")
    @GetMapping("/report")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'financial:report')")
    public Result<FinancialReportVO> generateReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(financialService.generateFinancialReport(startDate, endDate));
    }

    @Operation(summary = "Get daily report")
    @GetMapping("/report/daily")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'financial:report')")
    public Result<FinancialReportVO> getDailyReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.success(financialService.getDailyReport(date));
    }

    @Operation(summary = "Get monthly report")
    @GetMapping("/report/monthly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'financial:report')")
    public Result<FinancialReportVO> getMonthlyReport(@RequestParam Integer year, @RequestParam Integer month) {
        return Result.success(financialService.getMonthlyReport(year, month));
    }

    @Operation(summary = "Get yearly report")
    @GetMapping("/report/yearly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'financial:report')")
    public Result<FinancialReportVO> getYearlyReport(@RequestParam Integer year) {
        return Result.success(financialService.getYearlyReport(year));
    }

    @Operation(summary = "Export financial report")
    @GetMapping("/report/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'financial:report')")
    public ResponseEntity<byte[]> exportReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "csv") String format
    ) {
        byte[] data = financialService.exportFinancialReport(startDate, endDate, format);
        String filename = String.format("financial_report_%s_%s.csv", startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "Apply invoice")
    @PostMapping("/invoice/apply")
    public Result<Void> applyInvoice(@RequestParam Long orderId, @RequestBody Map<String, Object> invoiceData) {
        financialService.applyInvoice(getCurrentUserId(), orderId, invoiceData);
        return Result.success("Invoice applied successfully", null);
    }

    @Operation(summary = "Get my invoices")
    @GetMapping("/invoice/my")
    public Result<IPage<Invoice>> getMyInvoices(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String status
    ) {
        return Result.success(financialService.getInvoicePage(getCurrentUserId(), pageNum, pageSize, status));
    }

    @Operation(summary = "Get all invoices")
    @GetMapping("/invoice/all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'invoice:manage')")
    public Result<IPage<Invoice>> getAllInvoices(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String status
    ) {
        return Result.success(financialService.getInvoicePage(null, pageNum, pageSize, status));
    }

    @Operation(summary = "Get invoice detail")
    @GetMapping("/invoice/{id}")
    public Result<Invoice> getInvoiceDetail(@PathVariable Long id) {
        return Result.success(financialService.getInvoiceDetail(getCurrentUserId(), SecurityUtils.isAdmin(), id));
    }

    @Operation(summary = "Issue invoice")
    @PostMapping("/invoice/{id}/issue")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'invoice:manage')")
    public Result<Void> issueInvoice(@PathVariable Long id) {
        financialService.issueInvoice(id);
        return Result.success("Invoice issued successfully", null);
    }

    @Operation(summary = "Resend invoice")
    @PostMapping("/invoice/{id}/resend")
    public Result<Void> resendInvoice(@PathVariable Long id) {
        financialService.resendInvoice(getCurrentUserId(), SecurityUtils.isAdmin(), id);
        return Result.success("Invoice resent successfully", null);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
