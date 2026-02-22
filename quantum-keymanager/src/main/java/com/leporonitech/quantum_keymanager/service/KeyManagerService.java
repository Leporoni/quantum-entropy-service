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

/**
 * Serviço central para a lógica de negócio do gerenciamento de chaves RSA.
 *
 * Responsável por:
 * - Criar chaves RSA usando entropia quântica como semente (seed).
 * - Criptografar e descriptografar chaves privadas para armazenamento seguro.
 * - Gerenciar o ciclo de vida das chaves (listar, buscar, deletar).
 * - Implementar o protocolo de exportação segura (Key Wrapping).
 * - Monitorar e reportar o status da entropia quântica disponível.
 */
@Service
@RequiredArgsConstructor
public class KeyManagerService {

    private final QuantumDataRepository quantumDataRepository;
    private final RsaKeyRepository rsaKeyRepository;
    private final EncryptionService encryptionService;

    @Value("${WORKER_URL:#{null}}")
    private String workerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cria e armazena um novo par de chaves RSA.
     *
     * <p>Fluxo de execução:
     * 1. Consome 5 unidades de entropia quântica do banco de dados (Consume & Discard).
     * 2. Usa a entropia para inicializar um gerador de números pseudo-aleatórios (SHA1PRNG).
     * 3. Gera o par de chaves RSA com o tamanho especificado.
     * 4. Criptografa a chave privada com a Master Key do sistema (via EncryptionService).
     * 5. Salva a chave (com a privada criptografada) no repositório.
     *
     * @param alias um nome amigável para a chave.
     * @param keySize o tamanho da chave em bits (ex: 2048, 4096).
     * @return a entidade RsaKey salva, contendo a chave pública e a privada criptografada.
     * @throws InsufficientEntropyException se não houver entropia suficiente (menos de 5 unidades).
     * @throws Exception para outros erros de criptografia ou de banco de dados.
     */
    @Transactional
    public RsaKey createRsaKey(String alias, int keySize) throws Exception {
        // 1. Obter entropia do banco (precisamos de bytes suficientes para o seed)
        // Buscamos os últimos 5 registros de entropia para garantir boa aleatoriedade
        List<QuantumData> entropyBatch = quantumDataRepository.findUnusedDataWithLock(5);
        if (entropyBatch.isEmpty() || entropyBatch.size() < 5) {
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

        byte[] seed = seedBuffer.toByteArray();

        // 2. Inicializar SecureRandom com a semente quântica
        SecureRandom quantumRandom = SecureRandom.getInstance("SHA1PRNG");
        quantumRandom.setSeed(seed);

        // 3. Gerar par de chaves RSA
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize, quantumRandom);
        KeyPair pair = keyGen.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        // 4. Encriptar a chave privada com a Master Key
        String privateKeyEncrypted = encryptionService.encrypt(privateKeyBase64);

        // 5. Salvar no banco
        RsaKey rsaKey = new RsaKey();
        rsaKey.setAlias(alias);
        rsaKey.setPublicKey(publicKeyBase64);
        rsaKey.setPrivateKeyEncrypted(privateKeyEncrypted);
        rsaKey.setKeySize(keySize);

        return rsaKeyRepository.save(rsaKey);
    }

    /**
     * Retorna uma lista de todas as chaves RSA armazenadas.
     * A chave privada não é incluída nos objetos retornados.
     *
     * @return uma lista de entidades RsaKey.
     */
    public List<RsaKey> getAllKeys() {
        return rsaKeyRepository.findAll();
    }

    /**
     * Busca uma chave RSA específica pelo seu ID.
     *
     * @param id o ID da chave a ser buscada.
     * @return a entidade RsaKey correspondente.
     * @throws RuntimeException se a chave não for encontrada.
     */
    public RsaKey getKeyById(Long id) {
        return rsaKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Key not found"));
    }

    /**
     * Deleta uma chave RSA específica do banco de dados.
     * Esta operação é irreversível.
     *
     * @param id o ID da chave a ser deletada.
     * @throws RuntimeException se a chave não for encontrada.
     */
    @Transactional
    public void deleteKey(Long id) {
        if (!rsaKeyRepository.existsById(id)) {
            throw new RuntimeException("Key not found");
        }
        rsaKeyRepository.deleteById(id);
    }

    /**
     * Deleta todas as chaves RSA do banco de dados.
     * Esta operação é irreversível e limpa todo o cofre de chaves.
     */
    @Transactional
    public void deleteAllKeys() {
        rsaKeyRepository.deleteAll();
    }

    /**
     * Exporta uma chave privada de forma segura usando o protocolo de Key Wrapping.
     *
     * <p>Fluxo de execução:
     * 1. Busca a chave no banco e descriptografa a chave privada (protegida pela Master Key).
     * 2. Consome 2 unidades de nova entropia quântica para gerar uma chave de transporte temporária (AES-256).
     * 3. Criptografa a chave privada com esta chave de transporte.
     * 4. Retorna a chave privada criptografada junto com a chave de transporte.
     *
     * @param id o ID da chave a ser exportada.
     * @return um objeto KeyExportResponse contendo a chave privada criptografada e a chave de transporte.
     * @throws InsufficientEntropyException se não houver entropia suficiente para a chave de transporte.
     * @throws Exception para outros erros de criptografia.
     */
    @Transactional
    public KeyExportResponse exportPrivateKey(Long id) throws Exception {
        RsaKey rsaKey = getKeyById(id);

        // 1. Descriptografar a chave privada original (armazenada com Master Key)
        String privateKeyBase64 = encryptionService.decrypt(rsaKey.getPrivateKeyEncrypted());

        // 2. Obter nova entropia para a chave de transporte (AES-256 precisa de 32
        // bytes)
        List<QuantumData> transportEntropy = quantumDataRepository.findUnusedDataWithLock(2);
        if (transportEntropy.isEmpty() || transportEntropy.size() < 2) {
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

    /**
     * Obtém o status atual do "combustível" de entropia quântica.
     * Também aciona um "ping" para o worker de coleta se o nível estiver baixo.
     *
     * @return um objeto EntropyStatus com a contagem de registros disponíveis e os custos por operação.
     */
    public EntropyStatus getEntropyStatus() {
        long availableRecords = quantumDataRepository.countByUsedFalse();

        // Wake up worker if URL is configured & entropy is low (< 50)
        if (workerUrl != null && !workerUrl.isEmpty() && availableRecords < 50) {
            wakeWorker();
        }

        return new EntropyStatus(availableRecords, 5, 2);
    }

    /**
     * Envia uma requisição HTTP para "acordar" o worker de coleta de entropia.
     * Útil em plataformas com "cold starts" (como Render.com no plano gratuito).
     * Executa de forma assíncrona para não bloquear a thread principal.
     */
    private void wakeWorker() {
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.getForObject(workerUrl + "/health", String.class);
            } catch (Exception e) {
                // Ignore errors, it's just a wake-up ping
            }
        });
    }

    /**
     * DTO para o status da entropia.
     */
    @lombok.Value
    public static class EntropyStatus {
        long availableRecords;
        int costPerGeneration;
        int costPerExport;
    }

    /**
     * DTO para a resposta da exportação de chave.
     */
    @lombok.Value
    public static class KeyExportResponse {
        String encryptedPrivateKey;
        String transportKey;
        String algorithm;
    }
}
