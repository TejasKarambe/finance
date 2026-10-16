package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.request.AccountRequest;
import com.example.finance.dto.request.AccountUpdateRequest;
import com.example.finance.dto.request.TransferRequest;
import com.example.finance.dto.response.AccountResponse;
import com.example.finance.dto.response.TransferResponse;
import com.example.finance.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Endpoints for managing financial accounts and executing inter-account transfers")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create a new account for the authenticated user")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : request.userId();
        AccountResponse response = accountService.createAccount(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all accounts for the authenticated user")
    public ResponseEntity<List<AccountResponse>> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(accountService.getAccountsByUserId(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details by ID")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAccountById(id, principal.getId()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all accounts by user ID")
    public ResponseEntity<List<AccountResponse>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing account")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateAccount(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        accountService.deleteAccount(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @Operation(summary = "Execute atomic inter-account transfer without altering income or expense totals")
    public ResponseEntity<TransferResponse> transferFunds(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TransferResponse response = accountService.transferFunds(request, principal.getId());
        return ResponseEntity.ok(response);
    }
}