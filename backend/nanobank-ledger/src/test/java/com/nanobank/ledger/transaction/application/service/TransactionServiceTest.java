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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TransactionService transactionService;

    private User user;
    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        walletId = UUID.randomUUID();
        user = new User();
        user.setEmail("test@nanobank.com");
        user.setFullName("Test User");
        setId(user, userId, User.class);
    }

    private <T> void setId(T obj, UUID id, Class<T> clazz) {
        try {
            var field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Wallet buildWallet(BigDecimal balance, WalletStatus status) {
        Wallet wallet = new Wallet();
        setId(wallet, walletId, Wallet.class);
        wallet.setUser(user);
        wallet.setBalance(balance);
        wallet.setStatus(status);
        wallet.setCurrency("COP");
        return wallet;
    }

    private Transaction buildTransaction(Wallet wallet, TransactionType type, BigDecimal amount) {
        Transaction tx = new Transaction();
        setId(tx, UUID.randomUUID(), Transaction.class);
        tx.setWallet(wallet);
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency("COP");
        return tx;
    }

    @Nested
    @DisplayName("create() — INCOME")
    class CreateIncome {

        @Test
        @DisplayName("should credit wallet balance on INCOME")
        void shouldCreditOnIncome() {
            Wallet wallet = buildWallet(new BigDecimal("100.00"), WalletStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenReturn(wallet);
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                setId(tx, UUID.randomUUID(), Transaction.class);
                return tx;
            });

            var request = new CreateTransactionRequest(walletId, TransactionType.INCOME,
                    new BigDecimal("500.00"), "COP", "SALARY", "Monthly salary", null);

            TransactionResponse response = transactionService.create(userId, request);

            assertThat(response.type()).isEqualTo(TransactionType.INCOME);
            assertThat(response.amount()).isEqualByComparingTo("500.00");
            assertThat(wallet.getBalance()).isEqualByComparingTo("600.00");
        }
    }

    @Nested
    @DisplayName("create() — EXPENSE")
    class CreateExpense {

        @Test
        @DisplayName("should debit wallet balance on EXPENSE")
        void shouldDebitOnExpense() {
            Wallet wallet = buildWallet(new BigDecimal("1000.00"), WalletStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenReturn(wallet);
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                setId(tx, UUID.randomUUID(), Transaction.class);
                return tx;
            });

            var request = new CreateTransactionRequest(walletId, TransactionType.EXPENSE,
                    new BigDecimal("200.00"), "COP", "FOOD", "Groceries", null);

            TransactionResponse response = transactionService.create(userId, request);

            assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(wallet.getBalance()).isEqualByComparingTo("800.00");
        }

        @Test
        @DisplayName("should throw UnprocessableException when balance insufficient for EXPENSE")
        void shouldThrowWhenInsufficientBalance() {
            Wallet wallet = buildWallet(new BigDecimal("50.00"), WalletStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

            var request = new CreateTransactionRequest(walletId, TransactionType.EXPENSE,
                    new BigDecimal("200.00"), "COP", "FOOD", null, null);

            assertThatThrownBy(() -> transactionService.create(userId, request))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("Insufficient balance");
        }

        @Test
        @DisplayName("should throw UnprocessableException when wallet is inactive")
        void shouldThrowWhenWalletInactive() {
            Wallet wallet = buildWallet(new BigDecimal("500.00"), WalletStatus.INACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

            var request = new CreateTransactionRequest(walletId, TransactionType.EXPENSE,
                    new BigDecimal("100.00"), "COP", null, null, null);

            assertThatThrownBy(() -> transactionService.create(userId, request))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Nested
    @DisplayName("create() — validation")
    class CreateValidation {

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet does not belong to user")
        void shouldThrowWhenWalletNotOwnedByUser() {
            User otherUser = new User();
            setId(otherUser, UUID.randomUUID(), User.class);

            Wallet wallet = buildWallet(BigDecimal.TEN, WalletStatus.ACTIVE);
            wallet.setUser(otherUser);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

            var request = new CreateTransactionRequest(walletId, TransactionType.INCOME,
                    BigDecimal.ONE, "COP", null, null, null);

            assertThatThrownBy(() -> transactionService.create(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.empty());

            var request = new CreateTransactionRequest(walletId, TransactionType.INCOME,
                    BigDecimal.ONE, "COP", null, null, null);

            assertThatThrownBy(() -> transactionService.create(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByWallet()")
    class FindByWallet {

        @Test
        @DisplayName("should return paged transactions for wallet")
        void shouldReturnPagedTransactions() {
            Wallet wallet = buildWallet(BigDecimal.TEN, WalletStatus.ACTIVE);
            Transaction tx = buildTransaction(wallet, TransactionType.INCOME, new BigDecimal("100.00"));

            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1));

            PagedResponse<TransactionResponse> result =
                    transactionService.findByWallet(userId, walletId, null, null, null, null, 0, 20);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return transaction by id")
        void shouldReturnTransaction() {
            Wallet wallet = buildWallet(BigDecimal.TEN, WalletStatus.ACTIVE);
            Transaction tx = buildTransaction(wallet, TransactionType.EXPENSE, new BigDecimal("50.00"));
            UUID txId = tx.getId();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            TransactionResponse response = transactionService.findById(userId, txId);

            assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction not found")
        void shouldThrowWhenNotFound() {
            UUID txId = UUID.randomUUID();
            when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.findById(userId, txId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("move()")
    class Move {

        private UUID destWalletId;

        @BeforeEach
        void setUpMove() {
            destWalletId = UUID.randomUUID();
        }

        private Wallet buildDest(BigDecimal balance) {
            Wallet dest = new Wallet();
            setId(dest, destWalletId, Wallet.class);
            dest.setUser(user);
            dest.setBalance(balance);
            dest.setStatus(WalletStatus.ACTIVE);
            dest.setCurrency("COP");
            return dest;
        }

        @Test
        @DisplayName("should move INCOME transaction and adjust balances")
        void shouldMoveIncomeTransaction() {
            Wallet source = buildWallet(new BigDecimal("500.00"), WalletStatus.ACTIVE);
            Wallet dest   = buildDest(new BigDecimal("200.00"));
            Transaction tx = buildTransaction(source, TransactionType.INCOME, new BigDecimal("100.00"));
            UUID txId = tx.getId();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(destWalletId)).thenReturn(Optional.of(dest));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenReturn(tx);

            TransactionResponse response = transactionService.move(userId, txId, destWalletId);

            assertThat(source.getBalance()).isEqualByComparingTo("400.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("300.00");
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should move EXPENSE transaction and adjust balances")
        void shouldMoveExpenseTransaction() {
            Wallet source = buildWallet(new BigDecimal("500.00"), WalletStatus.ACTIVE);
            Wallet dest   = buildDest(new BigDecimal("200.00"));
            Transaction tx = buildTransaction(source, TransactionType.EXPENSE, new BigDecimal("80.00"));
            UUID txId = tx.getId();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(destWalletId)).thenReturn(Optional.of(dest));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenReturn(tx);

            transactionService.move(userId, txId, destWalletId);

            assertThat(source.getBalance()).isEqualByComparingTo("580.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("should throw UnprocessableException when source and destination are the same")
        void shouldThrowWhenSameWallet() {
            Wallet source = buildWallet(new BigDecimal("500.00"), WalletStatus.ACTIVE);
            Transaction tx = buildTransaction(source, TransactionType.INCOME, new BigDecimal("100.00"));
            UUID txId = tx.getId();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(source));

            assertThatThrownBy(() -> transactionService.move(userId, txId, walletId))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("different");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction not found")
        void shouldThrowWhenTransactionNotFound() {
            UUID txId = UUID.randomUUID();
            when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.move(userId, txId, destWalletId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw UnprocessableException when source wallet is inactive")
        void shouldThrowWhenSourceInactive() {
            Wallet source = buildWallet(new BigDecimal("500.00"), WalletStatus.INACTIVE);
            Wallet dest   = buildDest(new BigDecimal("200.00"));
            Transaction tx = buildTransaction(source, TransactionType.INCOME, new BigDecimal("100.00"));
            UUID txId = tx.getId();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
            when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(destWalletId)).thenReturn(Optional.of(dest));

            assertThatThrownBy(() -> transactionService.move(userId, txId, destWalletId))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("not active");
        }
    }
}
