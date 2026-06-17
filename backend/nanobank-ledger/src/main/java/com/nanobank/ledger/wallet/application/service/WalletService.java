package com.nanobank.ledger.wallet.application.service;

import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.auth.infrastructure.persistence.UserRepository;
import com.nanobank.ledger.shared.exception.ConflictException;
import com.nanobank.ledger.shared.exception.ResourceNotFoundException;
import com.nanobank.ledger.shared.exception.UnprocessableException;
import com.nanobank.ledger.wallet.application.dto.CreateWalletRequest;
import com.nanobank.ledger.wallet.application.dto.UpdateWalletRequest;
import com.nanobank.ledger.wallet.application.dto.WalletResponse;
import com.nanobank.ledger.wallet.domain.model.Wallet;
import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import com.nanobank.ledger.wallet.domain.model.WalletStatus;
import com.nanobank.ledger.wallet.infrastructure.persistence.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletResponse create(UUID userId, CreateWalletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (walletRepository.existsByUserIdAndNameAndStatus(userId, request.name(), WalletStatus.ACTIVE)) {
            throw new ConflictException(
                    "Wallet with name '" + request.name() + "' already exists",
                    "WALLET_NAME_DUPLICATE"
            );
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setName(request.name());
        wallet.setCategory(request.category());
        wallet.setBalance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO);
        wallet.setCurrency(request.currency() != null ? request.currency() : "COP");
        wallet.setDescription(request.description());

        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> findAll(UUID userId, WalletCategory category) {
        List<Wallet> wallets = category != null
                ? walletRepository.findByUserIdAndCategory(userId, category)
                : walletRepository.findByUserId(userId);
        return wallets.stream().map(WalletResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse findById(UUID userId, UUID walletId) {
        Wallet wallet = getWalletOwnedBy(userId, walletId);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse update(UUID userId, UUID walletId, UpdateWalletRequest request) {
        Wallet wallet = getWalletOwnedBy(userId, walletId);

        if (request.name() != null && !request.name().isBlank()) {
            wallet.setName(request.name());
        }
        if (request.category() != null) {
            wallet.setCategory(request.category());
        }
        if (request.description() != null) {
            wallet.setDescription(request.description());
        }

        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional
    public void deactivate(UUID userId, UUID walletId) {
        Wallet wallet = getWalletOwnedBy(userId, walletId);

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException(
                    "Cannot deactivate wallet with non-zero balance: " + wallet.getBalance(),
                    "WALLET_NOT_EMPTY"
            );
        }

        wallet.setStatus(WalletStatus.INACTIVE);
        walletRepository.save(wallet);
    }

    @Transactional
    public void transfer(UUID userId, UUID sourceId, UUID destinationId, BigDecimal amount) {
        if (sourceId.equals(destinationId)) {
            throw new UnprocessableException("Source and destination wallets must be different", "SAME_WALLET_TRANSFER");
        }

        // Pessimistic lock — ordered by ID para evitar deadlocks
        UUID firstLock = sourceId.compareTo(destinationId) < 0 ? sourceId : destinationId;
        UUID secondLock = firstLock.equals(sourceId) ? destinationId : sourceId;

        Wallet first = walletRepository.findByIdForUpdate(firstLock)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", firstLock.toString()));
        Wallet second = walletRepository.findByIdForUpdate(secondLock)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", secondLock.toString()));

        Wallet source = first.getId().equals(sourceId) ? first : second;
        Wallet destination = first.getId().equals(destinationId) ? first : second;

        validateOwnership(userId, source);
        validateOwnership(userId, destination);
        validateActive(source);
        validateActive(destination);

        if (source.getBalance().compareTo(amount) < 0) {
            throw new UnprocessableException(
                    "Insufficient balance. Available: " + source.getBalance() + ", Requested: " + amount,
                    "INSUFFICIENT_BALANCE"
            );
        }

        source.debit(amount);
        destination.credit(amount);

        walletRepository.save(source);
        walletRepository.save(destination);
    }

    private Wallet getWalletOwnedBy(UUID userId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));
        validateOwnership(userId, wallet);
        return wallet;
    }

    private void validateOwnership(UUID userId, Wallet wallet) {
        if (!wallet.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Wallet", wallet.getId().toString());
        }
    }

    private void validateActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new UnprocessableException("Wallet is not active: " + wallet.getId(), "WALLET_INACTIVE");
        }
    }
}
