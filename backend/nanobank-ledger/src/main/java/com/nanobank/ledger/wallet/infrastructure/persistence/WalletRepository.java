package com.nanobank.ledger.wallet.infrastructure.persistence;

import com.nanobank.ledger.wallet.domain.model.Wallet;
import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import com.nanobank.ledger.wallet.domain.model.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUserIdAndStatus(UUID userId, WalletStatus status);

    List<Wallet> findByUserId(UUID userId);

    List<Wallet> findByUserIdAndCategory(UUID userId, WalletCategory category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(UUID id);

    boolean existsByUserIdAndNameAndStatus(UUID userId, String name, WalletStatus status);
}
