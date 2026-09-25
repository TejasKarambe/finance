package com.example.finance.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 15, scale = 2) // Precision is the total number of digits, scale is the number of digits after the decimal
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY) // Many accounts can have one user
    @JoinColumn(name = "user_id", nullable = false) // Connect to user_id in the database
    private User user;   // The user who owns this account

    public Account() {
    }

    public Account(
            String name,
            String type,
            BigDecimal balance,
            User user
    ) {
        this.name = name;
        this.type = type;
        this.balance = balance;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}