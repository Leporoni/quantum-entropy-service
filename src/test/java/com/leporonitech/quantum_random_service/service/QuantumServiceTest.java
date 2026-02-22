package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuantumServiceTest {

    @Mock
    private LfdQuantumApiClient lfdQuantumApiClient;

    @InjectMocks
    private QuantumService quantumService;

    @Test
    void shouldReturnBase64StringWhenRequestIsValid() {
        // Arrange
        String hexString = "48656c6c6f"; // "Hello" in Hex
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(5);

        when(lfdQuantumApiClient.getRandomNumbers(5, "HEX")).thenReturn(mockResponse);

        // Act
        String result = quantumService.getQuantumNumbersAsBase64(5);

        // Assert
        assertEquals("SGVsbG8=", result); // "Hello" in Base64
        verify(lfdQuantumApiClient).getRandomNumbers(5, "HEX");
    }

    @Test
    void shouldThrowExceptionWhenCountIsTooHigh() {
        // Arrange
        int count = 1025;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(count);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCountIsTooLow() {
        // Arrange
        int count = 0;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(count);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenApiResponseIsNull() {
        // Arrange
        when(lfdQuantumApiClient.getRandomNumbers(anyInt(), anyString())).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(10);
        });

        assertEquals("Failed to fetch quantum random numbers from LfD API.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenQrnIsNull() {
        // Arrange
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(null);

        when(lfdQuantumApiClient.getRandomNumbers(10, "HEX")).thenReturn(mockResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(10);
        });

        assertEquals("Failed to fetch quantum random numbers from LfD API.", exception.getMessage());
    }
}