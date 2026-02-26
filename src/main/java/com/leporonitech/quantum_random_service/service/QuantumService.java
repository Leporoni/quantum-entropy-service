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

        return base64String;
    }

    /**
     * Mixes quantum entropy with local system entropy using SHA-256.
     * Following NIST SP 800-90C recommendations for entropy source composition.
     *
     * @param quantumBytes the quantum random bytes from LfD API
     * @return mixed entropy bytes
     */
    protected byte[] mixWithSystemEntropy(byte[] quantumBytes) {
        try {
            // Generate local system entropy of the same size
            byte[] systemEntropy = new byte[quantumBytes.length];
            SecureRandom.getInstanceStrong().nextBytes(systemEntropy);

            // Cryptographic mixing using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(quantumBytes);
            digest.update(systemEntropy);
            byte[] mixedSeed = digest.digest();

            log.debug("Mixed {} bytes of quantum entropy with {} bytes of system entropy. Output: {} bytes",
                    quantumBytes.length, systemEntropy.length, mixedSeed.length);

            return mixedSeed;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to mix entropy: SHA-256 or SecureRandom not available", e);
            throw new RuntimeException("Failed to mix entropy with system entropy", e);
        }
    }
}
