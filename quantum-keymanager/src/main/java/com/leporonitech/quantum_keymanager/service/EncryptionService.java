package com.leporonitech.quantum_keymanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Serviço de criptografia simétrica para proteger dados sensíveis em repouso.
 *
 * <p>Este serviço utiliza o algoritmo AES para criptografar e descriptografar
 * as chaves privadas RSA antes de serem persistidas no banco de dados. A segurança
 * é garantida por uma "Master Key" configurada via properties (`security.master-key`),
 * que serve como a chave secreta para todas as operações de criptografia.
 */
@Service
public class EncryptionService {

    @Value("${security.master-key}")
    private String masterKey;

    private static final String ALGORITHM = "AES";

    /**
     * Criptografa uma string usando AES com a Master Key do sistema.
     *
     * @param strToEncrypt a string em texto plano a ser criptografada.
     * @return a string criptografada e codificada em Base64.
     * @throws Exception se ocorrer um erro durante o processo de criptografia.
     */
    public String encrypt(String strToEncrypt) throws Exception {
        byte[] keyBytes = getSHA256Hash(masterKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Descriptografa uma string usando AES com a Master Key do sistema.
     *
     * @param strToDecrypt a string criptografada e codificada em Base64.
     * @return a string original em texto plano.
     * @throws Exception se ocorrer um erro durante o processo de descriptografia.
     */
    public String decrypt(String strToDecrypt) throws Exception {
        byte[] keyBytes = getSHA256Hash(masterKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)), StandardCharsets.UTF_8);
    }

    /**
     * Gera um hash SHA-256 da chave fornecida para garantir que ela tenha o tamanho
     * correto exigido pelo algoritmo AES (256 bits).
     *
     * @param key a chave de entrada (Master Key).
     * @return um array de bytes contendo o hash de 256 bits.
     * @throws Exception se o algoritmo SHA-256 não for encontrado.
     */
    private byte[] getSHA256Hash(String key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return sha.digest(keyBytes);
    }
}
