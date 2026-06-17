package com.nanobank.ledger.wallet.domain.model;

import com.nanobank.ledger.auth.domain.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletCategory category;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = WalletStatus.ACTIVE;
        if (currency == null) currency = "COP";
        if (balance == null) balance = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    // Getters
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public WalletCategory getCategory() { return category; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public WalletStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setUser(User user) { this.user = user; }
    public void setName(String name) { this.name = name; }
    public void setCategory(WalletCategory category) { this.category = category; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(WalletStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
}
