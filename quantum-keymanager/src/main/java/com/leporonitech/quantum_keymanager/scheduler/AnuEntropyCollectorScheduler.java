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
import jakarta.annotation.PostConstruct;

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

    @PostConstruct
    public void init() {
        log.info("🦘 ANU VACUUM COLLECTOR: Initializing warm-up cycle...");
        new Thread(() -> {
            try {
                Thread.sleep(15000); // Wait for NIST warm-up to finish and gateway to stabilize
                log.info("🦘 ANU VACUUM COLLECTOR: Running first startup fetch.");
                collectEntropy();
            } catch (Exception e) {
                log.error("❌ ANU VACUUM COLLECTOR: Initial fetch failed: {}", e.getMessage());
            }
        }).start();
    }

    @Scheduled(fixedDelay = 60000) // Check every 1 minute
    public void collectEntropy() {
        try {
            long count = quantumDataRepository.countByUsedFalseAndSource("ANU");

            if (count < 20) {
                log.info("🌀 ANU VACUUM API: Entropy low ({} units). Starting refill cycle...", count);

                int consecutiveFailures = 0;
                final int maxConsecutiveFailures = 3;

                while (count < 50 && consecutiveFailures < maxConsecutiveFailures) {
                    if (fetchAndSave()) {
                        count++;
                        consecutiveFailures = 0;
                        log.info("✅ ANU VACUUM API: Captured 256 bytes. Current reservoir: {} units", count);
                        sleep(2000); // 2s delay
                    } else {
                        consecutiveFailures++;
                        log.warn("⚠️ ANU VACUUM API: Refill failure #{}. Waiting 10s...", consecutiveFailures);
                        sleep(10000);
                    }
                }
                log.info("🏁 ANU VACUUM API: Refill cycle completed. Status: {} units", count);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ ANU VACUUM API: Critical error: {}", e.getMessage());
        }
    }

    private boolean fetchAndSave() {
        try {
            String url = apiBaseUrl + "/api/v1/quantum-random?source=ANU&count=256&pure=true";
            log.debug("🌀 ANU VACUUM API: Calling internal service...");
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            String dataBase64 = root.path("data").asText();

            if (dataBase64 != null && !dataBase64.isEmpty()) {
                byte[] rawBytes = Base64.getDecoder().decode(dataBase64);
                
                if (!entropyValidator.validate(rawBytes)) {
                    log.error("⚠️ ANU VACUUM API: Entropy validation failed: {}. Discarding.", entropyValidator.getFailureReason());
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
            log.error("❌ ANU VACUUM API: API communication error: {}", e.getMessage());
        }
        return false;
    }

    void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
