package com.leporonitech.quantum_keymanager.service;

import com.leporonitech.quantum_keymanager.exception.InsufficientEntropyException;
import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.model.RsaKey;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.repository.RsaKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KeyManagerService {

    private final QuantumDataRepository quantumDataRepository;
    private final RsaKeyRepository rsaKeyRepository;
    private final EncryptionService encryptionService;

    @Value("${WORKER_URL:#{null}}")
    private String workerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public RsaKey createRsaKey(String alias, int keySize) throws Exception {
        List<QuantumData> entropyBatch = quantumDataRepository.findUnusedDataWithLock(5);
        if (entropyBatch.isEmpty()) {
            long available = quantumDataRepository.countByUsedFalse();
            throw new InsufficientEntropyException(
                    String.format(
                            "Insufficient quantum entropy. The system requires at least 5 units to generate a key. Current available: %d.",
                            available));
        }

        java.io.ByteArrayOutputStream seedBuffer = new java.io.ByteArrayOutputStream();
        for (QuantumData data : entropyBatch) {
            byte[] chunk = Base64.getDecoder().decode(data.getDataBase64());
            seedBuffer.write(chunk);
            data.setUsed(true);
        }
        quantumDataRepository.saveAll(entropyBatch);

        byte[] quantumSeed = seedBuffer.toByteArray();
        byte[] systemEntropy = new byte[quantumSeed.length];
        SecureRandom.getInstanceStrong().nextBytes(systemEntropy);

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.update(quantumSeed);
        digest.update(systemEntropy);
        byte[] mixedSeed = digest.digest();

        SecureRandom combinedRandom;
        try {
            combinedRandom = SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e) {
            combinedRandom = SecureRandom.getInstanceStrong();
        }
        combinedRandom.setSeed(mixedSeed);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize, combinedRandom);
        KeyPair pair = keyGen.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String privateKeyEncrypted = encryptionService.encrypt(privateKeyBase64);

        RsaKey rsaKey = RsaKey.builder()
                .alias(alias)
                .publicKey(publicKeyBase64)
                .privateKeyEncrypted(privateKeyEncrypted)
                .keySize(keySize)
                .build();

        return rsaKeyRepository.save(rsaKey);
    }

    public List<RsaKey> getAllKeys() {
        return rsaKeyRepository.findAll();
    }

    public RsaKey getKeyById(Long id) {
        return rsaKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Key not found"));
    }

    @Transactional
    public void deleteKey(Long id) {
        if (!rsaKeyRepository.existsById(id)) {
            throw new RuntimeException("Key not found");
        }
        rsaKeyRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllKeys() {
        rsaKeyRepository.deleteAll();
    }

    @Transactional
    public KeyExportResponse exportPrivateKey(Long id) throws Exception {
        RsaKey rsaKey = getKeyById(id);
        String privateKeyBase64 = encryptionService.decrypt(rsaKey.getPrivateKeyEncrypted());

        List<QuantumData> transportEntropy = quantumDataRepository.findUnusedDataWithLock(2);
        if (transportEntropy.isEmpty()) {
            long available = quantumDataRepository.countByUsedFalse();
            throw new InsufficientEntropyException(
                    String.format(
                            "Insufficient quantum entropy for key export. Current available: %d.",
                            available));
        }

        java.io.ByteArrayOutputStream entropyBuffer = new java.io.ByteArrayOutputStream();
        for (QuantumData data : transportEntropy) {
            byte[] chunk = Base64.getDecoder().decode(data.getDataBase64());
            entropyBuffer.write(chunk);
            data.setUsed(true);
        }
        quantumDataRepository.saveAll(transportEntropy);

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] transportKeyBytes = sha.digest(entropyBuffer.toByteArray());
        String transportKeyBase64 = Base64.getEncoder().encodeToString(transportKeyBytes);

        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(transportKeyBytes, "AES");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedKey = cipher.doFinal(privateKeyBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return new KeyExportResponse(
                Base64.getEncoder().encodeToString(encryptedKey),
                transportKeyBase64,
                "AES-256");
    }

    public EntropyStatus getEntropyStatus() {
        // Encontra todos os registros não usados para calcular os bytes reais
        List<QuantumData> availableData = quantumDataRepository.findAll().stream()
                .filter(q -> !q.isUsed())
                .toList();

        long availableRecords = availableData.size();
        long availableBytes = availableData.stream()
                .mapToLong(q -> Base64.getDecoder().decode(q.getDataBase64()).length)
                .sum();

        // Acorda o worker se a entropia estiver baixa (< 200 registros)
        if (workerUrl != null && !workerUrl.isEmpty() && availableRecords < 200) {
            wakeWorker();
        }

        return new EntropyStatus(availableRecords, availableBytes, 5, 2);
    }

    private void wakeWorker() {
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.getForObject(workerUrl + "/health", String.class);
            } catch (Exception e) {
                // Ignore errors
            }
        });
    }

    @lombok.Value
    public static class EntropyStatus {
        long availableRecords;
        long availableBytes;
        int costPerGeneration;
        int costPerExport;
    }

    @lombok.Value
    public static class KeyExportResponse {
        String encryptedPrivateKey;
        String transportKey;
        String algorithm;
    }
}