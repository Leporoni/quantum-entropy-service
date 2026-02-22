package com.leporonitech.quantum_keymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientEntropyException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientEntropy(InsufficientEntropyException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "InsufficientEntropy");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
}
