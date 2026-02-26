package com.leporonitech.quantum_keymanager.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("memory")
@RequiredArgsConstructor
@Slf4j
public class EntropyCollectorScheduler {

    private final QuantumDataRepository quantumDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${API_BASE_URL:http://localhost:8081}")
    private String apiBaseUrl;

    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void collectEntropy() {
        try {
            long count = quantumDataRepository.countByUsedFalse();

            // Hysteresis logic: Refill if below 20, stop if above 50
            if (count < 20) {
                log.info("Entropy low ({}). Starting rapid refill...", count);

                int consecutiveFailures = 0;
                final int maxConsecutiveFailures = 10;

                // Fill up to 50
                while (count < 50 && consecutiveFailures < maxConsecutiveFailures) {
                    if (fetchAndSave()) {
                        count++;
                        consecutiveFailures = 0; // Reset on success
                        sleep(200); // 200ms delay between fetches (Fast Mode)
                    } else {
                        consecutiveFailures++;
                        sleep(2000); // Slow down on error
                    }
                }

                if (consecutiveFailures >= maxConsecutiveFailures) {
                    log.warn("Stopped refill after {} consecutive failures. Current count: {}", maxConsecutiveFailures, count);
                }
                log.info("Entropy refilled. Current count: {}", count);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to collect entropy: {}", e.getMessage());
        }
    }

    private boolean fetchAndSave() {
        try {
            // Request 128 bytes (same as Go client)
            String url = apiBaseUrl + "/api/v1/quantum-random?count=128";
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            String dataBase64 = root.path("data").asText();

            if (dataBase64 != null && !dataBase64.isEmpty()) {
                // NIST SP 800-90B Lite Validation
                if (!isValidEntropy(dataBase64)) {
                    log.error("Entropy validation failed. Discarding data.");
                    return false;
                }

                QuantumData data = new QuantumData();
                data.setDataBase64(dataBase64);
                data.setUsed(false);
                quantumDataRepository.save(data);
                return true;
            }
        } catch (Exception e) {
            log.error("Error fetching data from API: {}", e.getMessage());
        }
        return false;
    }

    // Comprehensive Entropy Validation (NIST SP 800-90B Lite)
    private boolean isValidEntropy(String base64Data) {
        try {
            byte[] rawBytes = java.util.Base64.getDecoder().decode(base64Data);

            // 1. Hex Decoding Attempt (If data is actually Hex string)
            byte[] data = tryDecodeHex(rawBytes);

            // 2. Minimum Length Check
            if (data.length < 32) {
                log.warn("Validation Failed: Data too short ({} bytes)", data.length);
                return false;
            }

            // 3. Repetition Check (Monobit / Runs test simplified)
            if (hasExcessiveRepetition(data)) {
                log.warn("Validation Failed: Excessive repetition detected");
                return false;
            }

            // 4. Shannon Entropy Test
            double entropy = calculateShannonEntropy(data);
            // Threshold: 6.0 bits/byte (max 8.0, but limited by sample size N=128)
            // A perfect distribution of 128 unique bytes yields 7.0 entropy.
            // Random collisions reduce this further, so 6.0 is a safe lower bound.
            if (entropy < 6.0) {
                log.warn("Validation Failed: Low Shannon Entropy ({})", String.format("%.2f", entropy));
                return false;
            }

            // 5. Compression Test (Deflate)
            // Random data should NOT be compressible.
            if (isCompressible(data)) {
                log.warn("Validation Failed: Data is compressible (pattern detected)");
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Validation Failed: Invalid Base64");
            return false;
        }
    }

    private byte[] tryDecodeHex(byte[] input) {
        // Check if bytes are printable ASCII Hex
        boolean isHex = true;
        if (input.length % 2 != 0)
            isHex = false;
        else {
            for (byte b : input) {
                if (!((b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F'))) {
                    isHex = false;
                    break;
                }
            }
        }

        if (isHex) {
            try {
                String hexString = new String(input);
                int len = hexString.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    // Convert hex pair to byte
                    int firstDigit = Character.digit(hexString.charAt(i), 16);
                    int secondDigit = Character.digit(hexString.charAt(i + 1), 16);
                    data[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
                }
                return data;
            } catch (Exception e) {
                return input; // Fallback
            }
        }
        return input;
    }

    private boolean hasExcessiveRepetition(byte[] data) {
        int repetitions = 0;
        for (int i = 1; i < data.length; i++) {
            if (data[i] == data[i - 1])
                repetitions++;
        }
        // Fail if > 20% bytes are repeated
        return repetitions > data.length * 0.2;
    }

    private double calculateShannonEntropy(byte[] data) {
        int[] frequencies = new int[256];
        for (byte b : data) {
            frequencies[b & 0xFF]++;
        }

        double entropy = 0;
        double total = data.length;

        for (int count : frequencies) {
            if (count > 0) {
                double p = count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private boolean isCompressible(byte[] data) {
        try {
            java.util.zip.Deflater deflater = new java.util.zip.Deflater();
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[data.length + 100];
            int compressedSize = deflater.deflate(buffer);
            deflater.end();

            // If compressed size is < 80% of original, it is compressible => Not random
            // Real random data usually expands when compressed due to overhead.
            return compressedSize < (data.length * 0.8);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sleep method that can be overridden in tests to avoid actual waiting.
     */
    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
