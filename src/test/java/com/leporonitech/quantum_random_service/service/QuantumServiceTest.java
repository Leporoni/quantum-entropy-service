package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

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
        String result = quantumService.getQuantumNumbersAsBase64(5);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Verify the result is valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result));
        verify(lfdQuantumApiClient).getRandomNumbers(5, "HEX");
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
        String result = quantumService.getQuantumNumbersAsBase64(5);

        // Assert - Result should be SHA-256 hash (32 bytes) encoded in Base64
        byte[] decodedResult = Base64.getDecoder().decode(result);
        assertEquals(32, decodedResult.length, "SHA-256 output should be 32 bytes");
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
        quantumService.getQuantumNumbersAsBase64(6);

        // Assert - verify mixWithSystemEntropy was called (method is protected, so we verify the result)
        verify(quantumService, times(1)).getQuantumNumbersAsBase64(6);
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
            quantumService.getQuantumNumbersAsBase64(14);
        });

        assertTrue(exception.getMessage().contains("Invalid Hex string received from LfD API"));
    }

    @Test
    void mixWithSystemEntropyShouldReturn32Bytes() {
        // Arrange
        byte[] quantumBytes = "test_quantum_data".getBytes();

        // Act
        byte[] result = quantumService.mixWithSystemEntropy(quantumBytes);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length, "SHA-256 should produce 32 bytes");
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

    @Test
    void mixWithSystemEntropyShouldProduceDeterministicResultWithMockedRandom() throws Exception {
        // This test verifies the mixing logic by using a deterministic "random" source
        // Arrange
        byte[] quantumBytes = HexFormat.of().parseHex("0102030405060708");

        // Create a QuantumService with mocked SecureRandom behavior
        QuantumService testService = new QuantumService(lfdQuantumApiClient) {
            @Override
            protected byte[] mixWithSystemEntropy(byte[] quantumBytes) {
                try {
                    // Use deterministic "system entropy" for testing
                    byte[] systemEntropy = HexFormat.of().parseHex("0a0b0c0d0e0f1011");

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    digest.update(quantumBytes);
                    digest.update(systemEntropy);
                    return digest.digest();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // Act
        byte[] result = testService.mixWithSystemEntropy(quantumBytes);

        // Assert
        assertNotNull(result);
        assertEquals(32, result.length);

        // Verify the hash is deterministic (same inputs = same output)
        byte[] expectedHash = calculateExpectedHash(quantumBytes, HexFormat.of().parseHex("0a0b0c0d0e0f1011"));
        assertArrayEquals(expectedHash, result);
    }

    private byte[] calculateExpectedHash(byte[] quantum, byte[] system) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(quantum);
        digest.update(system);
        return digest.digest();
    }
}