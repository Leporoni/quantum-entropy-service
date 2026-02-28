package com.leporonitech.quantum_keymanager.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.validation.EntropyValidatorComposite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Scheduler responsible for collecting and validating quantum entropy.
 * Implements hysteresis logic to maintain optimal entropy pool size.
 * 
 * Uses Strategy Pattern via EntropyValidatorComposite for NIST SP 800-90B validation.
 */
@Component
@Profile("memory")
@RequiredArgsConstructor
@Slf4j
public class EntropyCollectorScheduler {

    private final QuantumDataRepository quantumDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntropyValidatorComposite entropyValidator;

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
                // Decode Base64 and optionally hex
                byte[] rawBytes = Base64.getDecoder().decode(dataBase64);
                byte[] data = tryDecodeHex(rawBytes);

                // NIST SP 800-90B Lite Validation using Strategy Pattern
                if (!entropyValidator.validate(data)) {
                    log.error("Entropy validation failed: {}. Discarding data.", entropyValidator.getFailureReason());
                    return false;
                }

                QuantumData quantumData = new QuantumData();
                quantumData.setDataBase64(dataBase64);
                quantumData.setUsed(false);
                QuantumData savedData = quantumDataRepository.save(quantumData);
                log.info("Entropy saved to database successfully. ID: {}, Size: {} bytes", 
                    savedData.getId(), 
                    data.length);
                return true;
            }
        } catch (Exception e) {
            log.error("Error fetching data from API: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to decode hex-encoded data.
     * Some quantum APIs return hex strings instead of raw bytes.
     *
     * @param input the byte array to check
     * @return decoded bytes if input was hex, otherwise original input
     */
    private byte[] tryDecodeHex(byte[] input) {
        // Check if bytes are printable ASCII Hex
        boolean isHex = true;
        if (input.length % 2 != 0) {
            isHex = false;
        } else {
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

    /**
     * Sleep method that can be overridden in tests to avoid actual waiting.
     */
    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}