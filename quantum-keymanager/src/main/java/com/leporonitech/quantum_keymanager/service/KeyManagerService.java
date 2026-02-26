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
        // 1. Obter entropia do banco (precisamos de bytes suficientes para o seed)
        // Buscamos os últimos 5 registros de entropia para garantir boa aleatoriedade
        List<QuantumData> entropyBatch = quantumDataRepository.findUnusedDataWithLock(5);
        if (entropyBatch.isEmpty()) {
            long available = quantumDataRepository.countByUsedFalse();
            throw new InsufficientEntropyException(
                    String.format(
                            "Insufficient quantum entropy. The system requires at least 5 units to generate a key. Current available: %d. Please wait for the entropy collector to replenish resources.",
                            available));
        }

        java.io.ByteArrayOutputStream seedBuffer = new java.io.ByteArrayOutputStream();
        for (QuantumData data : entropyBatch) {
            // Decodificar cada pedaço individualmente
            byte[] chunk = Base64.getDecoder().decode(data.getDataBase64());
            seedBuffer.write(chunk);
            data.setUsed(true); // Marcar como usado (Consume & Discard)
        }
        quantumDataRepository.saveAll(entropyBatch);

        byte[] quantumSeed = seedBuffer.toByteArray();

        // 2. Obter entropia do sistema (Local Entropy) para mixagem (NIST Recommendation)
        byte[] systemEntropy = new byte[quantumSeed.length];
        SecureRandom.getInstanceStrong().nextBytes(systemEntropy);

        // 3. Mixagem Criptográfica (Quantum + System) usando SHA-512
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.update(quantumSeed);
        digest.update(systemEntropy);
        byte[] mixedSeed = digest.digest();

        // 4. Inicializar SecureRandom usando DRBG (NIST SP 800-90A)
        SecureRandom combinedRandom;
        try {
            // Tenta obter DRBG explicitamente (disponível no Java 9+)
            combinedRandom = SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e) {
            // Fallback para o gerador forte do sistema se DRBG específico não for encontrado
            combinedRandom = SecureRandom.getInstanceStrong();
        }
        combinedRandom.setSeed(mixedSeed);

        // 5. Gerar par de chaves RSA
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize, combinedRandom);
        KeyPair pair = keyGen.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        // 6. Encriptar a chave privada com a Master Key
        String privateKeyEncrypted = encryptionService.encrypt(privateKeyBase64);

        // 7. Salvar no banco
        RsaKey rsaKey = new RsaKey();
        rsaKey.setAlias(alias);
        rsaKey.setPublicKey(publicKeyBase64);
        rsaKey.setPrivateKeyEncrypted(privateKeyEncrypted);
        rsaKey.setKeySize(keySize);

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

        // 1. Descriptografar a chave privada original (armazenada com Master Key)
        String privateKeyBase64 = encryptionService.decrypt(rsaKey.getPrivateKeyEncrypted());

        // 2. Obter nova entropia para a chave de transporte (AES-256 precisa de 32
        // bytes)
        List<QuantumData> transportEntropy = quantumDataRepository.findUnusedDataWithLock(2);
        if (transportEntropy.isEmpty()) {
            long available = quantumDataRepository.countByUsedFalse();
            throw new InsufficientEntropyException(
                    String.format(
                            "Insufficient quantum entropy for key export. The system requires at least 2 units. Current available: %d. Please wait for the entropy collector to replenish resources.",
                            available));
        }

        java.io.ByteArrayOutputStream entropyBuffer = new java.io.ByteArrayOutputStream();
        for (QuantumData data : transportEntropy) {
            byte[] chunk = Base64.getDecoder().decode(data.getDataBase64());
            entropyBuffer.write(chunk);
            data.setUsed(true);
        }
        quantumDataRepository.saveAll(transportEntropy);

        // Usar SHA-256 para derivar uma chave de 256 bits da entropia
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] transportKeyBytes = sha.digest(entropyBuffer.toByteArray());
        String transportKeyBase64 = Base64.getEncoder().encodeToString(transportKeyBytes);

        // 3. Criptografar a chave privada com a chave de transporte
        // Aqui usamos um AES temporário
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
        long availableRecords = quantumDataRepository.countByUsedFalse();

        // Wake up worker if URL is configured & entropy is low (< 50)
        if (workerUrl != null && !workerUrl.isEmpty() && availableRecords < 50) {
            wakeWorker();
        }

        return new EntropyStatus(availableRecords, 5, 2);
    }

    private void wakeWorker() {
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.getForObject(workerUrl + "/health", String.class);
            } catch (Exception e) {
                // Ignore errors, it's just a wake-up ping
            }
        });
    }

    @lombok.Value
    public static class EntropyStatus {
        long availableRecords;
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
