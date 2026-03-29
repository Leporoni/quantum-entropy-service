package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.AnuQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.AnuApiResponse;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuantumService {

    private final LfdQuantumApiClient lfdQuantumApiClient;
    private final AnuQuantumApiClient anuQuantumApiClient;

    public String getQuantumNumbersAsBase64(String source, int count, boolean pure) {
        if ("ANU".equalsIgnoreCase(source)) {
            return getAnuNumbersAsBase64(count, pure);
        }
        return getLfdNumbersAsBase64(count, pure);
    }

    public String getLfdNumbersAsBase64(int count, boolean pure) {
        if (count <= 0 || count > 1024) {
            log.warn("Invalid 'count' requested: {}. Must be between 1 and 1024.", count);
            throw new IllegalArgumentException("Count must be between 1 and 1024.");
        }

        log.info("Fetching {} quantum random bytes from LfD API (Pure Mode: {}).", count, pure);
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

        return processEntropy(quantumBytes, pure);
    }

    public String getAnuNumbersAsBase64(int count, boolean pure) {
        if (count <= 0 || count > 1024) {
            throw new IllegalArgumentException("Count must be between 1 and 1024.");
        }

        log.info("Fetching {} quantum random bytes from ANU API (Pure Mode: {}).", count, pure);
        AnuApiResponse response = anuQuantumApiClient.getRandomNumbers(count, "uint8");

        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.error("Failed to fetch quantum random numbers from ANU API. Response: {}", response);
            throw new RuntimeException("Failed to fetch quantum random numbers from ANU API.");
        }

        List<Integer> data = response.getData();
        byte[] quantumBytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            quantumBytes[i] = data.get(i).byteValue();
        }

        return processEntropy(quantumBytes, pure);
    }

    private String processEntropy(byte[] quantumBytes, boolean pure) {
        byte[] finalEntropy;
        if (pure) {
            log.info("Returning PURE quantum entropy as requested for audit/lab.");
            finalEntropy = quantumBytes;
        } else {
            // NIST SP 800-90C: Mix quantum entropy with local system entropy for general use
            finalEntropy = mixWithSystemEntropy(quantumBytes);
        }

        String base64String = Base64.getEncoder().encodeToString(finalEntropy);
        log.info("Quantum entropy generation completed successfully. Mode: {}, Total bytes: {}", 
                 pure ? "PURE" : "MIXED", finalEntropy.length);

        return base64String;
    }

    /**
     * Mixes quantum entropy with local system entropy using a cryptographic
     * keystream approach. Following NIST SP 800-90C recommendations for
     * entropy source composition.
     */
    protected byte[] mixWithSystemEntropy(byte[] quantumBytes) {
        try {
            byte[] systemEntropy = new byte[32];
            SecureRandom.getInstanceStrong().nextBytes(systemEntropy);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(quantumBytes);
            digest.update(systemEntropy);
            byte[] mixingKey = digest.digest();

            byte[] keystream = generateKeystream(mixingKey, quantumBytes.length);

            byte[] mixedEntropy = new byte[quantumBytes.length];
            for (int i = 0; i < quantumBytes.length; i++) {
                mixedEntropy[i] = (byte) (quantumBytes[i] ^ keystream[i]);
            }

            log.debug("Mixed {} bytes of quantum entropy with system-derived keystream.", quantumBytes.length);
            return mixedEntropy;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to mix entropy: SHA-256 or SecureRandom not available", e);
            throw new RuntimeException("Failed to mix entropy with system entropy", e);
        }
    }

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