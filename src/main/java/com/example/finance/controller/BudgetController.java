package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.request.BudgetRequest;
import com.example.finance.dto.response.BudgetResponse;
import com.example.finance.dto.response.BudgetStatusResponse;
import com.example.finance.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Endpoints for managing monthly category budget limits and monitoring 80%/100% threshold alerts")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @Operation(summary = "Create or update a budget limit for a category and month")
    public ResponseEntity<BudgetResponse> createOrUpdateBudget(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        BudgetResponse response = budgetService.createOrUpdateBudget(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get user budgets, optionally filtered by month and year")
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(budgetService.getBudgets(userId, month, year));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(budgetService.getBudgetById(id, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget limit by ID")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        budgetService.deleteBudget(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Get live spending status and 80%/100% threshold alert levels for all budgeted categories")
    public ResponseEntity<List<BudgetStatusResponse>> getBudgetStatus(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : YearMonth.now().getMonthValue();
        int targetYear = (year != null && year >= 2000) ? year : YearMonth.now().getYear();

        return ResponseEntity.ok(budgetService.getBudgetStatuses(userId, targetMonth, targetYear));
    }
}
