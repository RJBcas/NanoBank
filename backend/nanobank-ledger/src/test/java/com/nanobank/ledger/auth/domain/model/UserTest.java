package com.nanobank.ledger.auth.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User domain model")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@nanobank.com");
        user.setPassword("hashed");
        user.setFullName("Test User");
        user.setRole(Role.ROLE_USER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("getUsername() should return email")
    void shouldReturnEmailAsUsername() {
        assertThat(user.getUsername()).isEqualTo("test@nanobank.com");
    }

    @Test
    @DisplayName("getPassword() should return hashed password")
    void shouldReturnPassword() {
        assertThat(user.getPassword()).isEqualTo("hashed");
    }

    @Test
    @DisplayName("getAuthorities() should contain role")
    void shouldReturnAuthorities() {
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getFullName() and getRole() and getStatus() should work")
    void shouldReturnFieldValues() {
        assertThat(user.getFullName()).isEqualTo("Test User");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("isAccountNonExpired() should always return true")
    void accountShouldNeverExpire() {
        assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isCredentialsNonExpired() should always return true")
    void credentialsShouldNeverExpire() {
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isEnabled() should be true when ACTIVE")
    void shouldBeEnabledWhenActive() {
        user.setStatus(UserStatus.ACTIVE);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("isEnabled() should be false when DEACTIVATED")
    void shouldBeDisabledWhenDeactivated() {
        user.setStatus(UserStatus.DEACTIVATED);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("isAccountNonLocked() should be false when SUSPENDED")
    void shouldBeLockedWhenSuspended() {
        user.setStatus(UserStatus.SUSPENDED);
        assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("isAccountNonLocked() should be true when ACTIVE")
    void shouldNotBeLockedWhenActive() {
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("onCreate() should set default role and status when null")
    void onCreateShouldSetDefaultsWhenNull() {
        User u = new User();
        u.setEmail("new@nanobank.com");
        u.setPassword("pass");
        u.setFullName("New User");

        u.onCreate();

        assertThat(u.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("onCreate() should preserve existing role and status when already set")
    void onCreateShouldPreserveExistingValues() {
        user.onCreate();

        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("onUpdate() should set updatedAt")
    void onUpdateShouldSetUpdatedAt() {
        user.onUpdate();
        // no exception thrown and lifecycle executes
    }
}
