package com.nanobank.ledger.shared.exception;

import com.nanobank.ledger.shared.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBusiness() should map status and code from BusinessException")
    void shouldHandleBusinessException() {
        var ex = new ResourceNotFoundException("Wallet", "123");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("handleBusiness() with ConflictException should return 409")
    void shouldHandle409Conflict() {
        var ex = new ConflictException("Duplicate wallet", "WALLET_NAME_DUPLICATE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("WALLET_NAME_DUPLICATE");
    }

    @Test
    @DisplayName("handleBusiness() with UnprocessableException should return 422")
    void shouldHandle422Unprocessable() {
        var ex = new UnprocessableException("Insufficient balance", "INSUFFICIENT_BALANCE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("handleValidation() should map field errors to map")
    void shouldHandleValidationErrors() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "email", "must not be blank"));
        var parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("shouldHandleValidationErrors"), -1);
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().data()).containsKey("email");
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("handleBadCredentials() should return 401")
    void shouldHandleBadCredentials() {
        var ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("handleAccessDenied() should return 403")
    void shouldHandleAccessDenied() {
        var ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("handleGeneral() should return 500 for unhandled exceptions")
    void shouldHandleGenericException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(new RuntimeException("oops"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
