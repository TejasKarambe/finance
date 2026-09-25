package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByCategory(String category);

    List<Transaction> findByAccountIdAndType(
            Long accountId,
            TransactionType type
    );
}