package com.nanobank.ledger.transaction.infrastructure.persistence;

import com.nanobank.ledger.transaction.domain.model.Transaction;
import com.nanobank.ledger.transaction.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.wallet.id = :walletId
              AND t.user.id = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:category IS NULL OR t.category = :category)
              AND (:dateFrom IS NULL OR t.occurredAt >= :dateFrom)
              AND (:dateTo IS NULL OR t.occurredAt <= :dateTo)
            ORDER BY t.occurredAt DESC, t.createdAt DESC
            """)
    Page<Transaction> findByFilters(
            @Param("walletId") UUID walletId,
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );
}
