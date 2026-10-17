package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.dto.response.YearlyReportResponse;
import com.example.finance.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Endpoints for generating financial monthly/yearly cash flow reports and CSV export")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Get monthly financial summary report")
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        int targetYear = (year != null && year >= 2000) ? year : YearMonth.now().getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : YearMonth.now().getMonthValue();

        return ResponseEntity.ok(reportService.getMonthlyReport(userId, targetYear, targetMonth));
    }

    @Operation(summary = "Get yearly financial summary report with 12-month trajectory")
    @GetMapping("/yearly")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        int targetYear = (year != null && year >= 2000) ? year : YearMonth.now().getYear();

        return ResponseEntity.ok(reportService.getYearlyReport(userId, targetYear));
    }

    @Operation(summary = "Get monthly category expense distribution")
    @GetMapping("/monthly/category")
    public ResponseEntity<List<CategoryExpenseResponse>> getCategoryExpenses(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        int targetYear = (year != null && year >= 2000) ? year : YearMonth.now().getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : YearMonth.now().getMonthValue();

        return ResponseEntity.ok(reportService.getCategoryExpenses(userId, targetYear, targetMonth));
    }

    @Operation(summary = "Export all user transactions to a CSV file")
    @GetMapping("/export/csv")
    public void exportTransactionsCsv(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response) throws IOException {
        Long userId = principal != null ? principal.getId() : null;

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String filename = "financial_ledger_" + LocalDate.now() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        reportService.exportTransactionsCsv(userId, response.getWriter());
    }
}