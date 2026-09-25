package com.example.finance.controller;

import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.request.TransactionUpdateRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.service.TransactionService;
import jakarta.validation.Valid;

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
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.getTransactions();
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(
            @PathVariable Long id) {
        return transactionService.getTransactionById(id);
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