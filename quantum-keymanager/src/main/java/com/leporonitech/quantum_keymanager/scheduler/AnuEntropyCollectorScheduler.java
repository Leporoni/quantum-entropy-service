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
 * Scheduler responsible for collecting ANU quantum entropy specifically for the Entropy Lab.
 * Maintains a smaller pool to respect ANU's rate limits.
 */
@Component
@Profile("memory")
@RequiredArgsConstructor
@Slf4j
public class AnuEntropyCollectorScheduler {

    private final QuantumDataRepository quantumDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntropyValidatorComposite entropyValidator;

    @Value("${API_BASE_URL:http://localhost:8081}")
    private String apiBaseUrl;

    @Scheduled(fixedDelay = 60000) // Check every 1 minute (ANU is slower/limited)
    public void collectEntropy() {
        try {
            long count = quantumDataRepository.countByUsedFalseAndSource("ANU");

            // Lower hysteresis for ANU: Refill if below 20, stop if above 50
            if (count < 20) {
                log.info("ANU Entropy low ({}). Starting refill...", count);

                int consecutiveFailures = 0;
                final int maxConsecutiveFailures = 3;

                while (count < 50 && consecutiveFailures < maxConsecutiveFailures) {
                    if (fetchAndSave()) {
                        count++;
                        consecutiveFailures = 0;
                        sleep(2000); // 2s delay between ANU fetches
                    } else {
                        consecutiveFailures++;
                        sleep(10000); // 10s delay on error
                    }
                }
                log.info("ANU Entropy refill cycle completed. Current count: {}", count);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to collect ANU entropy: {}", e.getMessage());
        }
    }

    private boolean fetchAndSave() {
        try {
            // Request 256 bytes of PURE ANU quantum entropy
            String url = apiBaseUrl + "/api/v1/quantum-random?source=ANU&count=256&pure=true";
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            String dataBase64 = root.path("data").asText();

            if (dataBase64 != null && !dataBase64.isEmpty()) {
                byte[] rawBytes = Base64.getDecoder().decode(dataBase64);
                
                // NIST SP 800-90B Validation
                if (!entropyValidator.validate(rawBytes)) {
                    log.error("ANU Entropy validation failed: {}. Discarding data.", entropyValidator.getFailureReason());
                    return false;
                }

                QuantumData quantumData = new QuantumData();
                quantumData.setDataBase64(dataBase64);
                quantumData.setUsed(false);
                quantumData.setSource("ANU");
                quantumDataRepository.save(quantumData);
                return true;
            }
        } catch (Exception e) {
            log.error("Error fetching ANU data: {}", e.getMessage());
        }
        return false;
    }

    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
