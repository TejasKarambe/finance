package com.example.finance.repository;

import com.example.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    Optional<Budget> findByUserIdAndCategoryAndMonthAndYear(Long userId, String category, int month, int year);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryAndMonthAndYear(Long userId, String category, int month, int year);

    void deleteByIdAndUserId(Long id, Long userId);
}
