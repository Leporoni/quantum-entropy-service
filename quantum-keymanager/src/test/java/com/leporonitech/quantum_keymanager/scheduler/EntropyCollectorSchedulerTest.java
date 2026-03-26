package com.leporonitech.quantum_keymanager.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.validation.EntropyValidatorComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Random;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntropyCollectorSchedulerTest {

    @Mock
    private QuantumDataRepository quantumDataRepository;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private EntropyValidatorComposite entropyValidator = new EntropyValidatorComposite();

    // Testable subclass that overrides sleep to avoid actual waiting
    private static class TestableEntropyCollectorScheduler extends EntropyCollectorScheduler {
        public TestableEntropyCollectorScheduler(QuantumDataRepository repo, RestTemplate restTemplate, ObjectMapper mapper, EntropyValidatorComposite validator) {
            super(repo, restTemplate, mapper, validator);
        }

        @Override
        void sleep(long millis) {
            // Do nothing - avoid actual sleep in tests
        }
    }

    private TestableEntropyCollectorScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TestableEntropyCollectorScheduler(quantumDataRepository, restTemplate, objectMapper, entropyValidator);
        ReflectionTestUtils.setField(scheduler, "apiBaseUrl", "http://localhost:8081");
    }

    @Test
    void shouldNotCollectWhenEntropyIsSufficient() {
        // Arrange - entropy count is above threshold (1000)
        when(quantumDataRepository.countByUsedFalse()).thenReturn(1200L);

        // Act
        scheduler.collectEntropy();

        // Assert - should not fetch new data
        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldCollectWhenEntropyIsLow() {
        // Arrange - entropy count is below threshold (200)
        when(quantumDataRepository.countByUsedFalse())
                .thenReturn(100L)  // First check - low
                .thenReturn(500L)  // After first fetch
                .thenReturn(1001L); // Above 1000, stop

        // Mock API response with valid random data
        String validBase64Data = generateValidRandomBase64(128);
        String apiResponse = "{\"data\":\"" + validBase64Data + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(quantumDataRepository.save(any(QuantumData.class))).thenReturn(new QuantumData());

        // Act
        scheduler.collectEntropy();

        // Assert - should have fetched new data
        verify(restTemplate, atLeastOnce()).getForObject(contains("/api/v1/quantum-random"), eq(String.class));
    }

    @Test
    void shouldValidateEntropyBeforeSaving() {
        // Arrange - start below threshold 200
        when(quantumDataRepository.countByUsedFalse()).thenReturn(190L).thenReturn(1001L);

        // Valid random data
        String validBase64Data = generateValidRandomBase64(128);
        String apiResponse = "{\"data\":\"" + validBase64Data + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(quantumDataRepository.save(any(QuantumData.class))).thenReturn(new QuantumData());

        // Act
        scheduler.collectEntropy();

        // Assert - data should be saved
        verify(quantumDataRepository, atLeastOnce()).save(argThat(data -> 
            data.getDataBase64() != null && !data.getDataBase64().isEmpty()
        ));
    }

    @Test
    void shouldRejectLowEntropyData() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);

        // Low entropy data (all same bytes - will fail Shannon entropy test)
        byte[] lowEntropyBytes = new byte[128];
        // All same byte - entropy will be 0
        String lowEntropyBase64 = Base64.getEncoder().encodeToString(lowEntropyBytes);
        String apiResponse = "{\"data\":\"" + lowEntropyBase64 + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);

        // Act
        scheduler.collectEntropy();

        // Assert - data should NOT be saved due to low entropy
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldRejectCompressibleData() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);

        // Highly compressible data (repeating pattern)
        byte[] compressibleBytes = new byte[128];
        for (int i = 0; i < 128; i++) {
            compressibleBytes[i] = (byte) (i % 4); // Only 4 unique values
        }
        String compressibleBase64 = Base64.getEncoder().encodeToString(compressibleBytes);
        String apiResponse = "{\"data\":\"" + compressibleBase64 + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);

        // Act
        scheduler.collectEntropy();

        // Assert - data should NOT be saved due to compressibility
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldRejectDataWithExcessiveRepetition() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);

        // Data with excessive repetition (> 20% consecutive same bytes)
        byte[] repetitiveBytes = new byte[128];
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            // 30% chance of repeating previous byte
            if (i > 0 && random.nextDouble() < 0.3) {
                repetitiveBytes[i] = repetitiveBytes[i - 1];
            } else {
                repetitiveBytes[i] = (byte) random.nextInt(256);
            }
        }
        // Force excessive repetition
        for (int i = 0; i < 50; i++) {
            repetitiveBytes[i] = 0x42; // 50 consecutive same bytes
        }
        String repetitiveBase64 = Base64.getEncoder().encodeToString(repetitiveBytes);
        String apiResponse = "{\"data\":\"" + repetitiveBase64 + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);

        // Act
        scheduler.collectEntropy();

        // Assert - data should NOT be saved due to repetition
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldHandleApiError() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("API Error"));

        // Act
        scheduler.collectEntropy();

        // Assert - should handle error gracefully
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldHandleInvalidJsonResponse() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("invalid json");

        // Act
        scheduler.collectEntropy();

        // Assert - should handle error gracefully
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyDataField() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"data\":\"\"}");

        // Act
        scheduler.collectEntropy();

        // Assert - should not save empty data
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldHandleMissingDataField() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"otherField\":\"value\"}");

        // Act
        scheduler.collectEntropy();

        // Assert - should handle missing field gracefully
        verify(quantumDataRepository, never()).save(any());
    }

    @Test
    void shouldRejectDataShorterThan32Bytes() {
        // Arrange
        when(quantumDataRepository.countByUsedFalse()).thenReturn(100L).thenReturn(1001L);

        // Only 16 bytes - below minimum
        String shortBase64 = Base64.getEncoder().encodeToString(new byte[16]);
        String apiResponse = "{\"data\":\"" + shortBase64 + "\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);

        // Act
        scheduler.collectEntropy();

        // Assert - should reject data that's too short
        verify(quantumDataRepository, never()).save(any());
    }

    /**
     * Generates valid random Base64 data that should pass all NIST SP 800-90B validation tests.
     */
    private String generateValidRandomBase64(int numBytes) {
        byte[] randomBytes = new byte[numBytes];
        Random random = new Random();
        random.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}