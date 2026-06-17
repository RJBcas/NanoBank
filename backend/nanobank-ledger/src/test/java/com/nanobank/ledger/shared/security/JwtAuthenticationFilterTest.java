package com.nanobank.ledger.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should pass through when no Authorization header")
    void shouldPassThroughWithNoHeader() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("should pass through when Authorization is not Bearer")
    void shouldPassThroughWithBasicAuth() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("should pass through when JWT extraction throws exception")
    void shouldPassThroughOnInvalidJwt() throws Exception {
        request.addHeader("Authorization", "Bearer bad.token.here");
        when(jwtService.extractUsername("bad.token.here")).thenThrow(new RuntimeException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should set authentication when JWT is valid")
    void shouldSetAuthenticationForValidJwt() throws Exception {
        UserDetails userDetails = User.withUsername("alice@nanobank.com")
                .password("pass").authorities(List.of()).build();

        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("alice@nanobank.com");
        when(userDetailsService.loadUserByUsername("alice@nanobank.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice@nanobank.com");
    }

    @Test
    @DisplayName("should not set authentication when token is invalid")
    void shouldNotSetAuthWhenTokenInvalid() throws Exception {
        UserDetails userDetails = User.withUsername("alice@nanobank.com")
                .password("pass").authorities(List.of()).build();

        request.addHeader("Authorization", "Bearer expired.token");
        when(jwtService.extractUsername("expired.token")).thenReturn("alice@nanobank.com");
        when(userDetailsService.loadUserByUsername("alice@nanobank.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("expired.token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
