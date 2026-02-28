package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuantumService {

    private final LfdQuantumApiClient lfdQuantumApiClient;

    public String getQuantumNumbersAsBase64(int count) {
        if (count <= 0 || count > 1024) {
            log.warn("Invalid 'count' requested: {}. Must be between 1 and 1024.", count);
            throw new IllegalArgumentException("Count must be between 1 and 1024.");
        }

        log.info("Fetching {} quantum random bytes from LfD API.", count);
        LfdApiResponse response = lfdQuantumApiClient.getRandomNumbers(count, "HEX");

        if (response == null || response.getQrn() == null) {
            log.error("Failed to fetch quantum random numbers. Response: {}", response);
            throw new RuntimeException("Failed to fetch quantum random numbers from LfD API.");
        }

        String hexString = response.getQrn();
        log.debug("Raw quantum hex string received from LfD API: {}", hexString);

        // Convert Hex string to byte array
        byte[] quantumBytes;
        try {
            quantumBytes = HexFormat.of().parseHex(hexString);
        } catch (IllegalArgumentException e) {
             log.error("Failed to parse Hex string from LfD API: {}", hexString, e);
             throw new RuntimeException("Invalid Hex string received from LfD API.", e);
        }

        // NIST SP 800-90C: Mix quantum entropy with local system entropy
        // This provides defense-in-depth in case the quantum source is compromised
        byte[] mixedEntropy = mixWithSystemEntropy(quantumBytes);

        String base64String = Base64.getEncoder().encodeToString(mixedEntropy);
        log.info("Generated Base64 string with mixed entropy (quantum + system): {}", base64String);
        log.info("Quantum entropy generation completed successfully. Total bytes: {}", mixedEntropy.length);

        return base64String;
    }

    /**
     * Mixes quantum entropy with local system entropy using a cryptographic
     * keystream approach. Following NIST SP 800-90C recommendations for
     * entropy source composition.
     *
     * This method preserves the original byte length by using SHA-256 to
     * derive a mixing key, then generating a keystream of the same size
     * and applying XOR.
     *
     * @param quantumBytes the quantum random bytes from LfD API
     * @return mixed entropy bytes (same length as input)
     */
    protected byte[] mixWithSystemEntropy(byte[] quantumBytes) {
        try {
            // Generate local system entropy for the mixing key
            byte[] systemEntropy = new byte[32];
            SecureRandom.getInstanceStrong().nextBytes(systemEntropy);

            // Derive a mixing key using SHA-256(quantum || system)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(quantumBytes);
            digest.update(systemEntropy);
            byte[] mixingKey = digest.digest();

            // Generate keystream of the same size as quantumBytes
            byte[] keystream = generateKeystream(mixingKey, quantumBytes.length);

            // XOR quantum bytes with keystream to produce mixed entropy
            byte[] mixedEntropy = new byte[quantumBytes.length];
            for (int i = 0; i < quantumBytes.length; i++) {
                mixedEntropy[i] = (byte) (quantumBytes[i] ^ keystream[i]);
            }

            log.debug("Mixed {} bytes of quantum entropy with system-derived keystream. Output: {} bytes",
                    quantumBytes.length, mixedEntropy.length);

            return mixedEntropy;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to mix entropy: SHA-256 or SecureRandom not available", e);
            throw new RuntimeException("Failed to mix entropy with system entropy", e);
        }
    }

    /**
     * Generates a keystream of the specified length using SHA-256 in counter mode.
     * This is a simple DRBG-like construction.
     *
     * @param key the key for keystream generation
     * @param length the desired keystream length
     * @return keystream bytes
     */
    private byte[] generateKeystream(byte[] key, int length) {
        byte[] keystream = new byte[length];
        int offset = 0;
        int counter = 0;

        try {
            while (offset < length) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(key);
                digest.update((byte) (counter >> 24));
                digest.update((byte) (counter >> 16));
                digest.update((byte) (counter >> 8));
                digest.update((byte) counter);

                byte[] block = digest.digest();
                int copyLength = Math.min(block.length, length - offset);
                System.arraycopy(block, 0, keystream, offset, copyLength);

                offset += copyLength;
                counter++;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        return keystream;
    }
}
