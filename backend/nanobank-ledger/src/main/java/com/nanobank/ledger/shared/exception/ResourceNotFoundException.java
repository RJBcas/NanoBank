package com.nanobank.ledger.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
