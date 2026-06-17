package com.nanobank.ledger.wallet.domain.model;

import com.nanobank.ledger.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Wallet domain model")
class WalletTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setCurrency("COP");
        wallet.setStatus(WalletStatus.ACTIVE);
    }

    @Test
    @DisplayName("credit() should increase balance")
    void shouldIncreaseBalanceOnCredit() {
        wallet.credit(new BigDecimal("500.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("debit() should decrease balance")
    void shouldDecreaseBalanceOnDebit() {
        wallet.debit(new BigDecimal("300.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("debit() should throw when balance is insufficient")
    void shouldThrowOnInsufficientBalance() {
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("2000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("debit() exact balance should result in zero")
    void debitExactBalanceShouldResultInZero() {
        wallet.debit(new BigDecimal("1000.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("multiple credits should accumulate correctly")
    void multipleCreditsAccumulate() {
        wallet.credit(new BigDecimal("100.00"));
        wallet.credit(new BigDecimal("200.00"));
        wallet.credit(new BigDecimal("50.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("1350.00");
    }

    @Test
    @DisplayName("credit then debit should compute correctly")
    void creditThenDebitShouldComputeCorrectly() {
        wallet.credit(new BigDecimal("500.00"));
        wallet.debit(new BigDecimal("750.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("getters and setters should reflect assigned values")
    void gettersAndSettersShouldWork() {
        User user = new User();
        wallet.setUser(user);
        wallet.setName("Savings");
        wallet.setCategory(WalletCategory.SAVINGS);
        wallet.setDescription("My savings wallet");

        assertThat(wallet.getUser()).isSameAs(user);
        assertThat(wallet.getName()).isEqualTo("Savings");
        assertThat(wallet.getCategory()).isEqualTo(WalletCategory.SAVINGS);
        assertThat(wallet.getDescription()).isEqualTo("My savings wallet");
        assertThat(wallet.getCurrency()).isEqualTo("COP");
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(wallet.getId()).isNull();
        assertThat(wallet.getCreatedAt()).isNull();
        assertThat(wallet.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("onCreate() should set defaults when status, currency, and balance are null")
    void onCreateShouldSetDefaultsWhenNull() {
        Wallet w = new Wallet();
        w.onCreate();

        assertThat(w.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(w.getCurrency()).isEqualTo("COP");
        assertThat(w.getBalance()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
        assertThat(w.getCreatedAt()).isNotNull();
        assertThat(w.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate() should preserve existing values when already set")
    void onCreateShouldPreserveExistingValues() {
        wallet.onCreate();

        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(wallet.getCurrency()).isEqualTo("COP");
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("onUpdate() should set updatedAt")
    void onUpdateShouldSetUpdatedAt() {
        wallet.onUpdate();
        assertThat(wallet.getUpdatedAt()).isNotNull();
    }
}
