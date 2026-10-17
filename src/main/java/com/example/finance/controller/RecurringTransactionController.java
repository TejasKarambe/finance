package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.request.RecurringTransactionRequest;
import com.example.finance.dto.response.RecurringTransactionResponse;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@Tag(name = "Recurring Transactions", description = "Endpoints for managing recurring subscriptions, bills, and salary schedules")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    public RecurringTransactionController(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @GetMapping
    @Operation(summary = "Get all recurring transactions for the authenticated user")
    public ResponseEntity<List<RecurringTransactionResponse>> getRecurringTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(recurringService.getRecurringTransactions(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring transaction by ID")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(recurringService.getRecurringTransaction(userId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new recurring transaction schedule")
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        RecurringTransactionResponse response = recurringService.createRecurringTransaction(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing recurring transaction")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(recurringService.updateRecurringTransaction(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recurring transaction schedule")
    public ResponseEntity<Void> deleteRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        recurringService.deleteRecurringTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Manually execute and post a recurring transaction to the ledger")
    public ResponseEntity<TransactionResponse> processRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        TransactionResponse response = recurringService.processRecurringTransaction(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-due")
    @Operation(summary = "Execute and post all due recurring transactions up to today")
    public ResponseEntity<List<TransactionResponse>> processAllDue(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        List<TransactionResponse> responses = recurringService.processAllDue(userId);
        return ResponseEntity.ok(responses);
    }
}
