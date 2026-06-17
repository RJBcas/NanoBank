package com.nanobank.ledger.auth.application.service;

import com.nanobank.ledger.auth.application.dto.AuthResponse;
import com.nanobank.ledger.auth.application.dto.LoginRequest;
import com.nanobank.ledger.auth.application.dto.RegisterRequest;
import com.nanobank.ledger.auth.domain.model.Role;
import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.auth.domain.model.UserStatus;
import com.nanobank.ledger.auth.infrastructure.persistence.UserRepository;
import com.nanobank.ledger.shared.exception.ConflictException;
import com.nanobank.ledger.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authManager;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("test@nanobank.com");
        user.setPassword("hashed");
        user.setFullName("Test User");
        user.setRole(Role.ROLE_USER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register user and return JWT")
        void shouldRegisterSuccessfully() {
            when(userRepository.existsByEmail("test@nanobank.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(user);
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            var request = new RegisterRequest("test@nanobank.com", "password123", "Test User");
            AuthResponse response = authService.register(request);

            assertThat(response.accessToken()).isEqualTo("jwt-token");
            assertThat(response.email()).isEqualTo("test@nanobank.com");
            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("should throw ConflictException when email already registered")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByEmail("test@nanobank.com")).thenReturn(true);

            var request = new RegisterRequest("test@nanobank.com", "password123", "Test User");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already registered");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should login successfully and return JWT")
        void shouldLoginSuccessfully() {
            when(authManager.authenticate(any())).thenReturn(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
            );
            when(userRepository.findByEmail("test@nanobank.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            var request = new LoginRequest("test@nanobank.com", "password123");
            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("should throw BadCredentialsException on wrong password")
        void shouldThrowOnBadCredentials() {
            when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            var request = new LoginRequest("test@nanobank.com", "wrongpassword");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUser {

        @Test
        @DisplayName("should return user by email")
        void shouldReturnUser() {
            when(userRepository.findByEmail("test@nanobank.com")).thenReturn(Optional.of(user));

            var result = authService.loadUserByUsername("test@nanobank.com");

            assertThat(result.getUsername()).isEqualTo("test@nanobank.com");
        }
    }
}
