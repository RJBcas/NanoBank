package com.nanobank.ledger.transaction.domain.model;

import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.wallet.domain.model.Wallet;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (occurredAt == null) occurredAt = LocalDate.now();
        if (currency == null) currency = "COP";
    }

    // Getters
    public UUID getId() { return id; }
    public Wallet getWallet() { return wallet; }
    public User getUser() { return user; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public void setUser(User user) { this.user = user; }
    public void setType(TransactionType type) { this.type = type; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setOccurredAt(LocalDate occurredAt) { this.occurredAt = occurredAt; }
}
