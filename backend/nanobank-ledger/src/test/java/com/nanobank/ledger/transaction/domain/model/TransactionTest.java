package com.nanobank.ledger.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction domain model")
class TransactionTest {

    @Test
    @DisplayName("onCreate() should set defaults when occurredAt and currency are null")
    void onCreateShouldSetDefaultsWhenNull() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(TransactionType.INCOME);

        tx.onCreate();

        assertThat(tx.getCreatedAt()).isNotNull();
        assertThat(tx.getOccurredAt()).isEqualTo(LocalDate.now());
        assertThat(tx.getCurrency()).isEqualTo("COP");
    }

    @Test
    @DisplayName("onCreate() should preserve existing occurredAt and currency when set")
    void onCreateShouldPreserveExistingValues() {
        Transaction tx = new Transaction();
        LocalDate customDate = LocalDate.of(2024, 1, 15);
        tx.setOccurredAt(customDate);
        tx.setCurrency("USD");

        tx.onCreate();

        assertThat(tx.getOccurredAt()).isEqualTo(customDate);
        assertThat(tx.getCurrency()).isEqualTo("USD");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getters should return assigned values")
    void gettersShouldWork() {
        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setCurrency("COP");
        tx.setCategory("FOOD");
        tx.setDescription("Groceries");
        tx.setOccurredAt(LocalDate.now());

        assertThat(tx.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(tx.getAmount()).isEqualByComparingTo("50.00");
        assertThat(tx.getCurrency()).isEqualTo("COP");
        assertThat(tx.getCategory()).isEqualTo("FOOD");
        assertThat(tx.getDescription()).isEqualTo("Groceries");
        assertThat(tx.getOccurredAt()).isEqualTo(LocalDate.now());
        assertThat(tx.getId()).isNull();
        assertThat(tx.getWallet()).isNull();
        assertThat(tx.getUser()).isNull();
        assertThat(tx.getCreatedAt()).isNull();
    }
}
