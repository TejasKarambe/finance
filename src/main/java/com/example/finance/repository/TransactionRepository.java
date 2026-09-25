package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.projection.CategoryExpenseProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByCategory(String category);

    List<Transaction> findByAccountIdAndType(
            Long accountId,
            TransactionType type);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.type = :type
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            """)
    BigDecimal getTotalByTypeAndMonth(
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

    @Query("""
            SELECT
                t.category AS category,
                SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.type = :type
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryExpenseProjection> getCategoryTotals(
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

}