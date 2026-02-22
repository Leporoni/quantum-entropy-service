package com.leporonitech.quantum_keymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientEntropyException extends RuntimeException {
    public InsufficientEntropyException(String message) {
        super(message);
    }
}
