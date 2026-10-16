package com.example.finance.repository.specification;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }

    public static Specification<Transaction> descriptionContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Transaction> hasAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> amountGreaterThanOrEqualTo(BigDecimal amount) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), amount);
    }

    public static Specification<Transaction> amountLessThanOrEqualTo(BigDecimal amount) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), amount);
    }

    public static Specification<Transaction> dateGreaterThanOrEqualTo(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    public static Specification<Transaction> dateLessThanOrEqualTo(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }
}