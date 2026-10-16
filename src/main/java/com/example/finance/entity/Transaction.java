package com.example.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_account", columnList = "account_id"),
        @Index(name = "idx_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_category", columnList = "category"),
        @Index(name = "idx_transaction_user", columnList = "user_id")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 30)
    private String paymentMethod = "OTHER";

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Transaction() {
    }

    public Transaction(
            BigDecimal amount,
            TransactionType type,
            String category,
            String description,
            LocalDate transactionDate,
            LocalDateTime createdAt,
            Account account) {
        this(amount, type, category, description, "OTHER", transactionDate, createdAt, account, account != null ? account.getUser() : null);
    }

    public Transaction(
            BigDecimal amount,
            TransactionType type,
            String category,
            String description,
            String paymentMethod,
            LocalDate transactionDate,
            LocalDateTime createdAt,
            Account account,
            User user) {
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.paymentMethod = paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "OTHER";
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
        this.account = account;
        this.user = user != null ? user : (account != null ? account.getUser() : null);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (this.user == null && account != null) {
            this.user = account.getUser();
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}