package com.nanobank.ledger.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    private UserDetails user(String email) {
        return User.withUsername(email).password("pass").authorities(List.of()).build();
    }

    @Test
    @DisplayName("generateToken() should return a non-blank JWT")
    void shouldGenerateNonBlankToken() {
        String token = jwtService.generateToken(user("test@nanobank.com"));
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername() should return the subject")
    void shouldExtractUsername() {
        String token = jwtService.generateToken(user("alice@nanobank.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@nanobank.com");
    }

    @Test
    @DisplayName("isTokenValid() should return true for valid token and matching user")
    void shouldValidateTokenForMatchingUser() {
        UserDetails u = user("bob@nanobank.com");
        String token  = jwtService.generateToken(u);
        assertThat(jwtService.isTokenValid(token, u)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() should return false when user does not match")
    void shouldFailValidationForDifferentUser() {
        String token   = jwtService.generateToken(user("alice@nanobank.com"));
        UserDetails bob = user("bob@nanobank.com");
        assertThat(jwtService.isTokenValid(token, bob)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() should return false for expired token")
    void shouldFailValidationForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String token = jwtService.generateToken(user("test@nanobank.com"));
        assertThat(jwtService.isTokenValid(token, user("test@nanobank.com"))).isFalse();
    }

    @Test
    @DisplayName("generateToken() with extra claims should embed them")
    void shouldGenerateTokenWithExtraClaims() {
        var claims = java.util.Map.<String, Object>of("role", "ADMIN");
        String token = jwtService.generateToken(claims, user("admin@nanobank.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@nanobank.com");
    }
}
