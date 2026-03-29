package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuantumServiceTest {

    @Mock
    private LfdQuantumApiClient lfdQuantumApiClient;

    @Spy
    @InjectMocks
    private QuantumService quantumService;

    @Test
    void shouldReturnBase64StringWhenRequestIsValid() {
        // Arrange
        String hexString = "48656c6c6f"; // "Hello" in Hex (5 bytes)
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(5);

        when(lfdQuantumApiClient.getRandomNumbers(5, "HEX")).thenReturn(mockResponse);

        // Act
        String result = quantumService.getQuantumNumbersAsBase64("LFD", 5, false);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Verify the result is valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result));
        verify(lfdQuantumApiClient).getRandomNumbers(5, "HEX");
    }

    @Test
    void shouldReturnPureQuantumEntropyWhenPureModeIsTrue() {
        // Arrange
        String hexString = "48656c6c6f"; // "Hello" in Hex (5 bytes)
        byte[] expectedBytes = {0x48, 0x65, 0x6c, 0x6c, 0x6f};
        String expectedBase64 = Base64.getEncoder().encodeToString(expectedBytes);

        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(5);

        when(lfdQuantumApiClient.getRandomNumbers(5, "HEX")).thenReturn(mockResponse);

        // Act
        String result = quantumService.getQuantumNumbersAsBase64("LFD", 5, true);

        // Assert
        assertEquals(expectedBase64, result, "In PURE mode, the result should be exactly the bytes from LfD API");
        verify(quantumService, never()).mixWithSystemEntropy(any());
    }

    @Test
    void shouldMixQuantumEntropyWithSystemEntropy() {
        // Arrange
        String hexString = "48656c6c6f"; // "Hello" in Hex (5 bytes)
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(5);

        when(lfdQuantumApiClient.getRandomNumbers(5, "HEX")).thenReturn(mockResponse);

        // Act
        String result = quantumService.getQuantumNumbersAsBase64("LFD", 5, false);

        // Assert - Result should preserve original length (5 bytes) after mixing
        byte[] decodedResult = Base64.getDecoder().decode(result);
        assertEquals(5, decodedResult.length, "Output should preserve original input length");
    }

    @Test
    void shouldCallMixWithSystemEntropyMethod() {
        // Arrange
        String hexString = "a1b2c3d4e5f6"; // 6 bytes
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(6);

        when(lfdQuantumApiClient.getRandomNumbers(6, "HEX")).thenReturn(mockResponse);

        // Act
        quantumService.getQuantumNumbersAsBase64("LFD", 6, false);

        // Assert - verify mixWithSystemEntropy was called
        verify(quantumService, times(1)).mixWithSystemEntropy(any());
    }

    @Test
    void shouldThrowExceptionWhenCountIsTooHigh() {
        // Arrange
        int count = 1025;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64("LFD", count, false);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCountIsTooLow() {
        // Arrange
        int count = 0;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64("LFD", count, false);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenApiResponseIsNull() {
        // Arrange
        when(lfdQuantumApiClient.getRandomNumbers(anyInt(), anyString())).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64("LFD", 10, false);
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
            quantumService.getQuantumNumbersAsBase64("LFD", 10, false);
        });

        assertEquals("Failed to fetch quantum random numbers from LfD API.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenInvalidHexString() {
        // Arrange
        String invalidHexString = "NOT_VALID_HEX!!"; // Invalid hex characters
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(invalidHexString);
        mockResponse.setLength(14);

        when(lfdQuantumApiClient.getRandomNumbers(14, "HEX")).thenReturn(mockResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64("LFD", 14, false);
        });

        assertTrue(exception.getMessage().contains("Invalid Hex string received from LfD API"));
    }

    @Test
    void mixWithSystemEntropyShouldPreserveInputLength() {
        // Arrange
        byte[] quantumBytes = "test_quantum_data".getBytes(); // 17 bytes

        // Act
        byte[] result = quantumService.mixWithSystemEntropy(quantumBytes);

        // Assert
        assertNotNull(result);
        assertEquals(quantumBytes.length, result.length, "Output should preserve input length");
    }

    @Test
    void mixWithSystemEntropyShouldPreserve128Bytes() {
        // Arrange - 128 bytes is the typical quantum request size
        byte[] quantumBytes = new byte[128];
        new SecureRandom().nextBytes(quantumBytes);

        // Act
        byte[] result = quantumService.mixWithSystemEntropy(quantumBytes);

        // Assert
        assertNotNull(result);
        assertEquals(128, result.length, "Output should preserve 128-byte input length");
    }

    @Test
    void mixWithSystemEntropyShouldProduceDifferentResultsOnEachCall() {
        // Arrange
        byte[] quantumBytes = "same_quantum_input".getBytes();

        // Act
        byte[] result1 = quantumService.mixWithSystemEntropy(quantumBytes);
        byte[] result2 = quantumService.mixWithSystemEntropy(quantumBytes);

        // Assert - Results should be different due to random system entropy
        assertNotEquals(
                Base64.getEncoder().encodeToString(result1),
                Base64.getEncoder().encodeToString(result2),
                "Each call should produce different results due to system entropy"
        );
    }
}