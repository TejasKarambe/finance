package com.example.finance.controller;

import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.request.TransactionUpdateRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.service.TransactionService;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public TransactionResponse createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @GetMapping
    public Page<TransactionResponse> getTransactions(
            @ParameterObject
            @PageableDefault(size = 10, sort = "transactionDate", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        if (sortBy != null && !sortBy.isBlank()) {
            if ("accountId".equalsIgnoreCase(sortBy)) {
                sortBy = "account.id";
            }
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, sortBy));
        }

        return transactionService.getTransactions(pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(
            @PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponse> getTransactionsByAccountId(
            @PathVariable Long accountId) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @GetMapping("/type/{type}")
    public List<TransactionResponse> getTransactionsByType(
            @PathVariable TransactionType type) {
        return transactionService.getTransactionsByType(type);
    }

    @GetMapping("/category/{category}")
    public List<TransactionResponse> getTransactionsByCategory(
            @PathVariable String category) {
        return transactionService.getTransactionsByCategory(category);
    }

    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        return transactionService.updateTransaction(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id) {
        transactionService.deleteTransaction(id);

        return ResponseEntity.noContent().build();
    }

}