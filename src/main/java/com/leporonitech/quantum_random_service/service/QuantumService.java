package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        byte[] bytes;
        try {
            bytes = HexFormat.of().parseHex(hexString);
        } catch (IllegalArgumentException e) {
             log.error("Failed to parse Hex string from LfD API: {}", hexString, e);
             throw new RuntimeException("Invalid Hex string received from LfD API.", e);
        }

        String base64String = Base64.getEncoder().encodeToString(bytes);
        log.info("Generated Base64 string: {}", base64String);

        return base64String;
    }
}