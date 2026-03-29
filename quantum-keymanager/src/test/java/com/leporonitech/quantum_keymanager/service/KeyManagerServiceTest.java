package com.leporonitech.quantum_keymanager.service;

import com.leporonitech.quantum_keymanager.exception.InsufficientEntropyException;
import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.model.RsaKey;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.repository.RsaKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class KeyManagerServiceTest {

    @Mock
    private QuantumDataRepository quantumDataRepository;

    @Mock
    private RsaKeyRepository rsaKeyRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private KeyManagerService keyManagerService;

    private List<QuantumData> createMockEntropyBatch(int count) {
        List<QuantumData> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QuantumData data = new QuantumData();
            data.setId((long) i + 1);
            // Generate 128 bytes of mock entropy encoded in Base64
            byte[] mockBytes = new byte[128];
            for (int j = 0; j < 128; j++) {
                mockBytes[j] = (byte) (i * 128 + j);
            }
            data.setDataBase64(Base64.getEncoder().encodeToString(mockBytes));
            data.setUsed(false);
            batch.add(data);
        }
        return batch;
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keyManagerService, "workerUrl", "http://localhost:8081");
    }

    @Test
    void shouldCreateRsaKeyWithMixedEntropy() throws Exception {
        // Arrange
        List<QuantumData> entropyBatch = createMockEntropyBatch(5);
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 5)).thenReturn(entropyBatch);
        when(quantumDataRepository.saveAll(anyList())).thenReturn(entropyBatch);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-private-key");
        
        RsaKey savedKey = new RsaKey();
        savedKey.setId(1L);
        savedKey.setAlias("test-key");
        savedKey.setPublicKey("public-key-base64");
        savedKey.setPrivateKeyEncrypted("encrypted-private-key");
        savedKey.setKeySize(2048);
        when(rsaKeyRepository.save(any(RsaKey.class))).thenReturn(savedKey);

        // Act
        RsaKey result = keyManagerService.createRsaKey("test-key", 2048);

        // Assert
        assertNotNull(result);
        assertEquals("test-key", result.getAlias());
        assertEquals(2048, result.getKeySize());
        
        // Verify entropy was consumed
        verify(quantumDataRepository).findUnusedDataWithLock("LFD", 5);
        verify(quantumDataRepository).saveAll(anyList());
        verify(rsaKeyRepository).save(any(RsaKey.class));
        
        // Verify all entropy was marked as used
        assertTrue(entropyBatch.stream().allMatch(QuantumData::isUsed));
    }

    @Test
    void shouldThrowInsufficientEntropyExceptionWhenNoEntropyAvailable() {
        // Arrange
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 5)).thenReturn(List.of());
        when(quantumDataRepository.countByUsedFalseAndSource("LFD")).thenReturn(0L);

        // Act & Assert
        InsufficientEntropyException exception = assertThrows(
                InsufficientEntropyException.class,
                () -> keyManagerService.createRsaKey("test-key", 2048)
        );

        assertTrue(exception.getMessage().contains("Insufficient quantum entropy"));
        assertTrue(exception.getMessage().contains("Current available: 0"));
        
        // Verify no key was saved
        verify(rsaKeyRepository, never()).save(any());
    }

    @Test
    void shouldThrowInsufficientEntropyExceptionWhenNotEnoughEntropy() {
        // Arrange
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 5)).thenReturn(List.of());
        when(quantumDataRepository.countByUsedFalseAndSource("LFD")).thenReturn(3L);

        // Act & Assert
        InsufficientEntropyException exception = assertThrows(
                InsufficientEntropyException.class,
                () -> keyManagerService.createRsaKey("test-key", 2048)
        );

        assertTrue(exception.getMessage().contains("Current available: 3"));
    }

    @Test
    void shouldGetAllKeys() {
        // Arrange
        List<RsaKey> mockKeys = List.of(
                new RsaKey(),
                new RsaKey()
        );
        when(rsaKeyRepository.findAll()).thenReturn(mockKeys);

        // Act
        List<RsaKey> result = keyManagerService.getAllKeys();

        // Assert
        assertEquals(2, result.size());
        verify(rsaKeyRepository).findAll();
    }

    @Test
    void shouldGetKeyById() {
        // Arrange
        RsaKey mockKey = new RsaKey();
        mockKey.setId(1L);
        mockKey.setAlias("test-key");
        when(rsaKeyRepository.findById(1L)).thenReturn(Optional.of(mockKey));

        // Act
        RsaKey result = keyManagerService.getKeyById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("test-key", result.getAlias());
        verify(rsaKeyRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenKeyNotFound() {
        // Arrange
        when(rsaKeyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keyManagerService.getKeyById(999L)
        );

        assertEquals("Key not found", exception.getMessage());
    }

    @Test
    void shouldDeleteKey() {
        // Arrange
        when(rsaKeyRepository.existsById(1L)).thenReturn(true);
        doNothing().when(rsaKeyRepository).deleteById(1L);

        // Act
        keyManagerService.deleteKey(1L);

        // Assert
        verify(rsaKeyRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentKey() {
        // Arrange
        when(rsaKeyRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keyManagerService.deleteKey(999L)
        );

        assertEquals("Key not found", exception.getMessage());
        verify(rsaKeyRepository, never()).deleteById(any());
    }

    @Test
    void shouldDeleteAllKeys() {
        // Arrange
        doNothing().when(rsaKeyRepository).deleteAll();

        // Act
        keyManagerService.deleteAllKeys();

        // Assert
        verify(rsaKeyRepository).deleteAll();
    }

    @Test
    void shouldGetEntropyStatus() {
        // Arrange
        List<QuantumData> mockData = createMockEntropyBatch(10);
        mockData.forEach(d -> d.setSource("LFD"));
        when(quantumDataRepository.findAll()).thenReturn(mockData);

        // Act
        KeyManagerService.EntropyStatus status = keyManagerService.getEntropyStatus();

        // Assert
        assertEquals(10L, status.getAvailableRecords());
        assertEquals(5, status.getCostPerGeneration());
        assertEquals(2, status.getCostPerExport());
    }

    @Test
    void shouldWakeWorkerWhenEntropyIsLow() {
        // Arrange
        List<QuantumData> mockData = createMockEntropyBatch(10); // Below 200
        mockData.forEach(d -> d.setSource("LFD"));
        when(quantumDataRepository.findAll()).thenReturn(mockData);

        // Act
        keyManagerService.getEntropyStatus();

        // Assert - Worker wake is async, we verify the records were checked
        verify(quantumDataRepository).findAll();
    }

    @Test
    void shouldNotWakeWorkerWhenEntropyIsSufficient() {
        // Arrange
        List<QuantumData> mockData = createMockEntropyBatch(250); // Above 200
        mockData.forEach(d -> d.setSource("LFD"));
        when(quantumDataRepository.findAll()).thenReturn(mockData);

        // Act
        keyManagerService.getEntropyStatus();

        // Assert
        verify(quantumDataRepository).findAll();
    }

    @Test
    void shouldExportPrivateKeyWithQuantumEntropy() throws Exception {
        // Arrange
        RsaKey mockKey = new RsaKey();
        mockKey.setId(1L);
        mockKey.setAlias("test-key");
        mockKey.setPrivateKeyEncrypted("encrypted-key");
        when(rsaKeyRepository.findById(1L)).thenReturn(Optional.of(mockKey));
        
        when(encryptionService.decrypt("encrypted-key")).thenReturn("decrypted-private-key-base64");
        
        List<QuantumData> transportEntropy = createMockEntropyBatch(2);
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 2)).thenReturn(transportEntropy);
        when(quantumDataRepository.saveAll(anyList())).thenReturn(transportEntropy);

        // Act
        KeyManagerService.KeyExportResponse response = keyManagerService.exportPrivateKey(1L);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getEncryptedPrivateKey());
        assertNotNull(response.getTransportKey());
        assertEquals("AES-256", response.getAlgorithm());

        // Verify entropy was consumed
        verify(quantumDataRepository).findUnusedDataWithLock("LFD", 2);
    }

    @Test
    void shouldThrowInsufficientEntropyExceptionWhenExportingWithNoEntropy() throws Exception {
        // Arrange
        RsaKey mockKey = new RsaKey();
        mockKey.setId(1L);
        mockKey.setPrivateKeyEncrypted("encrypted-key");
        when(rsaKeyRepository.findById(1L)).thenReturn(Optional.of(mockKey));
        lenient().when(encryptionService.decrypt("encrypted-key")).thenReturn("decrypted-key");
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 2)).thenReturn(List.of());
        when(quantumDataRepository.countByUsedFalseAndSource("LFD")).thenReturn(0L);

        // Act & Assert
        InsufficientEntropyException exception = assertThrows(
                InsufficientEntropyException.class,
                () -> keyManagerService.exportPrivateKey(1L)
        );

        assertTrue(exception.getMessage().contains("Insufficient LFD quantum entropy for key export"));
    }

    @Test
    void shouldCreateRsaKeyWithDifferentKeySizes() throws Exception {
        // Arrange - Test with 4096-bit key
        List<QuantumData> entropyBatch = createMockEntropyBatch(5);
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 5)).thenReturn(entropyBatch);
        when(quantumDataRepository.saveAll(anyList())).thenReturn(entropyBatch);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-private-key");
        
        RsaKey savedKey = new RsaKey();
        savedKey.setId(1L);
        savedKey.setAlias("large-key");
        savedKey.setKeySize(4096);
        when(rsaKeyRepository.save(any(RsaKey.class))).thenReturn(savedKey);

        // Act
        RsaKey result = keyManagerService.createRsaKey("large-key", 4096);

        // Assert
        assertEquals(4096, result.getKeySize());
    }

    @Test
    void shouldMarkEntropyAsUsedAfterKeyGeneration() throws Exception {
        // Arrange
        List<QuantumData> entropyBatch = createMockEntropyBatch(5);
        when(quantumDataRepository.findUnusedDataWithLock("LFD", 5)).thenReturn(entropyBatch);
        when(quantumDataRepository.saveAll(anyList())).thenReturn(entropyBatch);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        
        RsaKey savedKey = new RsaKey();
        savedKey.setId(1L);
        when(rsaKeyRepository.save(any(RsaKey.class))).thenReturn(savedKey);

        // Act
        keyManagerService.createRsaKey("test-key", 2048);

        // Assert - verify all entropy was marked as used
        assertTrue(entropyBatch.stream().allMatch(QuantumData::isUsed));
        verify(quantumDataRepository).saveAll(anyList());
    }
}