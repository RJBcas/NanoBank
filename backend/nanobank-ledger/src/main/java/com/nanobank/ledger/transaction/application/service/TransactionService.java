package com.nanobank.ledger.transaction.application.service;

import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.auth.infrastructure.persistence.UserRepository;
import com.nanobank.ledger.shared.exception.ResourceNotFoundException;
import com.nanobank.ledger.shared.exception.UnprocessableException;
import com.nanobank.ledger.transaction.application.dto.CreateTransactionRequest;
import com.nanobank.ledger.transaction.application.dto.PagedResponse;
import com.nanobank.ledger.transaction.application.dto.TransactionResponse;
import com.nanobank.ledger.transaction.domain.model.Transaction;
import com.nanobank.ledger.transaction.domain.model.TransactionType;
import com.nanobank.ledger.transaction.infrastructure.persistence.TransactionRepository;
import com.nanobank.ledger.wallet.domain.model.Wallet;
import com.nanobank.ledger.wallet.domain.model.WalletStatus;
import com.nanobank.ledger.wallet.infrastructure.persistence.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransactionResponse create(UUID userId, CreateTransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Wallet wallet = walletRepository.findByIdForUpdate(request.walletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", request.walletId().toString()));

        validateOwnership(userId, wallet);
        validateActive(wallet);

        if (request.type() == TransactionType.EXPENSE || request.type() == TransactionType.TRANSFER_OUT) {
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new UnprocessableException(
                        "Insufficient balance. Available: " + wallet.getBalance(),
                        "INSUFFICIENT_BALANCE"
                );
            }
            wallet.debit(request.amount());
        } else {
            wallet.credit(request.amount());
        }

        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency() != null ? request.currency() : wallet.getCurrency());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : LocalDate.now());

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> findByWallet(
            UUID userId,
            UUID walletId,
            TransactionType type,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        validateOwnership(userId, wallet);

        var pageable = PageRequest.of(page, size);
        var result = transactionRepository.findByFilters(walletId, userId, type, category, dateFrom, dateTo, pageable);
        return PagedResponse.from(result.map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID userId, UUID transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));
        validateOwnership(userId, tx.getWallet());
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse move(UUID userId, UUID transactionId, UUID destinationWalletId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        validateOwnership(userId, tx.getWallet());

        Wallet source = walletRepository.findByIdForUpdate(tx.getWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", tx.getWallet().getId().toString()));
        Wallet destination = walletRepository.findByIdForUpdate(destinationWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", destinationWalletId.toString()));

        validateOwnership(userId, destination);
        validateActive(source);
        validateActive(destination);

        if (source.getId().equals(destination.getId())) {
            throw new UnprocessableException("Source and destination wallets must be different", "SAME_WALLET_MOVE");
        }

        // Revert effect on source wallet
        if (tx.getType() == com.nanobank.ledger.transaction.domain.model.TransactionType.INCOME
                || tx.getType() == com.nanobank.ledger.transaction.domain.model.TransactionType.TRANSFER_IN) {
            source.debit(tx.getAmount());
            destination.credit(tx.getAmount());
        } else {
            source.credit(tx.getAmount());
            destination.debit(tx.getAmount());
        }

        walletRepository.save(source);
        walletRepository.save(destination);

        tx.setWallet(destination);
        return TransactionResponse.from(transactionRepository.save(tx));
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
