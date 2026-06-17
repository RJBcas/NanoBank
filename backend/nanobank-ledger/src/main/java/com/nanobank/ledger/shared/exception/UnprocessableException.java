package com.nanobank.ledger.shared.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableException extends BusinessException {

    public UnprocessableException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }
}
