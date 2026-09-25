package com.example.finance.controller;

import com.example.finance.dto.response.CategoryExpenseResponse;
import com.example.finance.dto.response.MonthlyReportResponse;
import com.example.finance.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report Controller", description = "API for generating financial reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Get monthly report")
    @GetMapping("/monthly")
    public MonthlyReportResponse getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        return reportService.getMonthlyReport(year, month);
    }

    @GetMapping("/monthly/category")
    public List<CategoryExpenseResponse> getCategoryExpenses(
            @RequestParam int year,
            @RequestParam int month) {
        return reportService.getCategoryExpenses(year, month);
    }
}