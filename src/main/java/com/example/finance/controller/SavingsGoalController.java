package com.example.finance.controller;

import com.example.finance.config.security.UserPrincipal;
import com.example.finance.dto.request.GoalContributionRequest;
import com.example.finance.dto.request.SavingsGoalRequest;
import com.example.finance.dto.response.SavingsGoalResponse;
import com.example.finance.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
@Tag(name = "Savings Goals", description = "Endpoints for creating and tracking savings targets and making account contributions")
public class SavingsGoalController {

    private final SavingsGoalService goalService;

    public SavingsGoalController(SavingsGoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    @Operation(summary = "Get all savings goals for the authenticated user")
    public ResponseEntity<List<SavingsGoalResponse>> getGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(goalService.getGoals(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get savings goal by ID")
    public ResponseEntity<SavingsGoalResponse> getGoalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(goalService.getGoal(userId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new savings goal target")
    public ResponseEntity<SavingsGoalResponse> createGoal(
            @Valid @RequestBody SavingsGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        SavingsGoalResponse response = goalService.createGoal(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing savings goal")
    public ResponseEntity<SavingsGoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(goalService.updateGoal(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        goalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    @Operation(summary = "Contribute funds from an account toward a savings goal")
    public ResponseEntity<SavingsGoalResponse> contribute(
            @PathVariable Long id,
            @Valid @RequestBody GoalContributionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        SavingsGoalResponse response = goalService.contribute(userId, id, request);
        return ResponseEntity.ok(response);
    }
}
