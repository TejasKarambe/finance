package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.request.TransactionFilterRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.request.TransactionUpdateRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Endpoints for managing transaction ledger, filtering, and searches")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Record a new transaction and update the account balance")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        TransactionResponse response = transactionService.createTransaction(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get paginated, sorted, and filtered transactions for the authenticated user")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedSortBy = sortBy;
        if ("accountId".equalsIgnoreCase(sortBy)) {
            resolvedSortBy = "account.id";
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));

        TransactionFilterRequest filter = new TransactionFilterRequest(
                search,
                type,
                category,
                accountId,
                minAmount,
                maxAmount,
                startDate,
                endDate
        );

        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(transactionService.getTransactions(filter, pageable, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(transactionService.getTransactionById(id, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transaction and atomically adjust account balance")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(transactionService.updateTransaction(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction and reverse account balance adjustment")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions with query parameters")
    public ResponseEntity<Page<TransactionResponse>> searchTransactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {

        return getTransactions(search, type, category, accountId, minAmount, maxAmount, startDate, endDate, page, size, sortBy, sortDir, principal);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account ID")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get transactions by type")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByType(@PathVariable TransactionType type) {
        return ResponseEntity.ok(transactionService.getTransactionsByType(type));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get transactions by category")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(transactionService.getTransactionsByCategory(category));
    }
}