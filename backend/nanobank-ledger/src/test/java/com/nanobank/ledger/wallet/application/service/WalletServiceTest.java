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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WalletService walletService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setEmail("test@nanobank.com");
        user.setFullName("Test User");
    }

    private User userWithId(UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private Wallet buildWallet(UUID walletId, User owner, BigDecimal balance, WalletStatus status) {
        Wallet wallet = new Wallet();
        try {
            var idField = Wallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(wallet, walletId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        wallet.setUser(owner);
        wallet.setName("Test Wallet");
        wallet.setCategory(WalletCategory.SAVINGS);
        wallet.setBalance(balance);
        wallet.setStatus(status);
        wallet.setCurrency("COP");
        return wallet;
    }

    @Nested
    @DisplayName("create()")
    class CreateWallet {

        @Test
        @DisplayName("should create wallet successfully with initial balance")
        void shouldCreateWalletWithInitialBalance() {
            userWithId(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserIdAndNameAndStatus(userId, "Savings", WalletStatus.ACTIVE)).thenReturn(false);
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new CreateWalletRequest("Savings", WalletCategory.SAVINGS, new BigDecimal("500.00"), "COP", null);
            WalletResponse response = walletService.create(userId, request);

            assertThat(response.balance()).isEqualByComparingTo("500.00");
            assertThat(response.category()).isEqualTo(WalletCategory.SAVINGS);
            assertThat(response.currency()).isEqualTo("COP");
        }

        @Test
        @DisplayName("should create wallet with zero balance when no initial balance provided")
        void shouldCreateWalletWithZeroBalance() {
            userWithId(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserIdAndNameAndStatus(any(), any(), any())).thenReturn(false);
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new CreateWalletRequest("Expenses", WalletCategory.EXPENSES, null, "COP", null);
            WalletResponse response = walletService.create(userId, request);

            assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should default currency to COP when null")
        void shouldDefaultCurrencyToCOP() {
            userWithId(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserIdAndNameAndStatus(any(), any(), any())).thenReturn(false);
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new CreateWalletRequest("Checking", WalletCategory.EXPENSES, null, null, null);
            WalletResponse response = walletService.create(userId, request);

            assertThat(response.currency()).isEqualTo("COP");
        }

        @Test
        @DisplayName("should throw ConflictException when wallet name already exists")
        void shouldThrowConflictWhenNameDuplicated() {
            userWithId(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserIdAndNameAndStatus(userId, "Savings", WalletStatus.ACTIVE)).thenReturn(true);

            var request = new CreateWalletRequest("Savings", WalletCategory.SAVINGS, null, "COP", null);

            assertThatThrownBy(() -> walletService.create(userId, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            var request = new CreateWalletRequest("Savings", WalletCategory.SAVINGS, null, "COP", null);

            assertThatThrownBy(() -> walletService.create(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("should return all wallets for user")
        void shouldReturnAllWallets() {
            userWithId(userId);
            Wallet w1 = buildWallet(UUID.randomUUID(), user, BigDecimal.TEN, WalletStatus.ACTIVE);
            Wallet w2 = buildWallet(UUID.randomUUID(), user, BigDecimal.ONE, WalletStatus.ACTIVE);
            when(walletRepository.findByUserId(userId)).thenReturn(List.of(w1, w2));

            List<WalletResponse> result = walletService.findAll(userId, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should filter wallets by category")
        void shouldFilterByCategory() {
            userWithId(userId);
            Wallet savings = buildWallet(UUID.randomUUID(), user, BigDecimal.TEN, WalletStatus.ACTIVE);
            when(walletRepository.findByUserIdAndCategory(userId, WalletCategory.SAVINGS)).thenReturn(List.of(savings));

            List<WalletResponse> result = walletService.findAll(userId, WalletCategory.SAVINGS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo(WalletCategory.SAVINGS);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateWallet {

        @Test
        @DisplayName("should update wallet name")
        void shouldUpdateName() {
            userWithId(userId);
            UUID walletId = UUID.randomUUID();
            Wallet wallet = buildWallet(walletId, user, BigDecimal.ZERO, WalletStatus.ACTIVE);
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new UpdateWalletRequest("New Name", null, null);
            WalletResponse response = walletService.update(userId, walletId, request);

            assertThat(response.name()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should update category and description when provided")
        void shouldUpdateCategoryAndDescription() {
            userWithId(userId);
            UUID walletId = UUID.randomUUID();
            Wallet wallet = buildWallet(walletId, user, BigDecimal.ZERO, WalletStatus.ACTIVE);
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new UpdateWalletRequest(null, WalletCategory.EXPENSES, "Updated description");
            WalletResponse response = walletService.update(userId, walletId, request);

            assertThat(response.category()).isEqualTo(WalletCategory.EXPENSES);
            assertThat(response.description()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateWallet {

        @Test
        @DisplayName("should deactivate wallet when balance is zero")
        void shouldDeactivateWithZeroBalance() {
            userWithId(userId);
            UUID walletId = UUID.randomUUID();
            Wallet wallet = buildWallet(walletId, user, BigDecimal.ZERO, WalletStatus.ACTIVE);
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.deactivate(userId, walletId);

            verify(walletRepository).save(argThat(w -> w.getStatus() == WalletStatus.INACTIVE));
        }

        @Test
        @DisplayName("should throw ConflictException when balance is non-zero")
        void shouldThrowWhenBalanceNotEmpty() {
            userWithId(userId);
            UUID walletId = UUID.randomUUID();
            Wallet wallet = buildWallet(walletId, user, new BigDecimal("100.00"), WalletStatus.ACTIVE);
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.deactivate(userId, walletId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("non-zero balance");
        }
    }

    @Nested
    @DisplayName("transfer()")
    class Transfer {

        @Test
        @DisplayName("should transfer funds between two wallets")
        void shouldTransferSuccessfully() {
            userWithId(userId);
            UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID destId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

            Wallet source = buildWallet(sourceId, user, new BigDecimal("1000.00"), WalletStatus.ACTIVE);
            Wallet dest   = buildWallet(destId,   user, new BigDecimal("200.00"),  WalletStatus.ACTIVE);

            when(walletRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(destId)).thenReturn(Optional.of(dest));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.transfer(userId, sourceId, destId, new BigDecimal("300.00"));

            assertThat(source.getBalance()).isEqualByComparingTo("700.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("should throw UnprocessableException when same wallet")
        void shouldThrowWhenSameWallet() {
            UUID walletId = UUID.randomUUID();

            assertThatThrownBy(() -> walletService.transfer(userId, walletId, walletId, BigDecimal.ONE))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("different");
        }

        @Test
        @DisplayName("should throw UnprocessableException when insufficient balance")
        void shouldThrowWhenInsufficientBalance() {
            userWithId(userId);
            UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID destId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

            Wallet source = buildWallet(sourceId, user, new BigDecimal("50.00"), WalletStatus.ACTIVE);
            Wallet dest   = buildWallet(destId,   user, BigDecimal.ZERO,          WalletStatus.ACTIVE);

            when(walletRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
            when(walletRepository.findByIdForUpdate(destId)).thenReturn(Optional.of(dest));

            assertThatThrownBy(() -> walletService.transfer(userId, sourceId, destId, new BigDecimal("200.00")))
                    .isInstanceOf(UnprocessableException.class)
                    .hasMessageContaining("Insufficient balance");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when source wallet not found")
        void shouldThrowWhenSourceNotFound() {
            UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID destId   = UUID.fromString("00000000-0000-0000-0000-000000000002");

            when(walletRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.transfer(userId, sourceId, destId, BigDecimal.ONE))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should acquire locks in reverse order when sourceId > destId")
        void shouldTransferWhenSourceIdGreaterThanDest() {
            userWithId(userId);
            // sourceId > destId → lock order: dest first, then source
            UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            UUID destId   = UUID.fromString("00000000-0000-0000-0000-000000000001");

            Wallet source = buildWallet(sourceId, user, new BigDecimal("500.00"), WalletStatus.ACTIVE);
            Wallet dest   = buildWallet(destId,   user, new BigDecimal("100.00"), WalletStatus.ACTIVE);

            when(walletRepository.findByIdForUpdate(destId)).thenReturn(Optional.of(dest));
            when(walletRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.transfer(userId, sourceId, destId, new BigDecimal("200.00"));

            assertThat(source.getBalance()).isEqualByComparingTo("300.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("300.00");
        }
    }
}
