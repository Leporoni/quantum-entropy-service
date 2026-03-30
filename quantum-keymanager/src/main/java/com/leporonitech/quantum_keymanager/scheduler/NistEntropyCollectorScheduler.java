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
 * Scheduler responsible for collecting NIST Randomness Beacon pulses.
 * These pulses are used as a "Public Photonic Reference" in the Entropy Lab.
 */
@Component
@Profile("memory")
@RequiredArgsConstructor
@Slf4j
public class NistEntropyCollectorScheduler {

    private final QuantumDataRepository quantumDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntropyValidatorComposite entropyValidator;

    @Value("${API_BASE_URL:http://localhost:8081}")
    private String apiBaseUrl;

    @PostConstruct
    public void init() {
        log.info("🚀 NIST PHOTONIC COLLECTOR: Initializing warm-up cycle...");
        new Thread(() -> {
            try {
                Thread.sleep(10000); // Wait for gateway and quantum-service to be ready
                log.info("🚀 NIST PHOTONIC COLLECTOR: Running first startup fetch.");
                collectEntropy();
            } catch (Exception e) {
                log.error("❌ NIST PHOTONIC COLLECTOR: Initial fetch failed: {}", e.getMessage());
            }
        }).start();
    }

    @Scheduled(fixedDelay = 45000) // Check more frequently to catch pulses (NIST updates every 1m)
    public void collectEntropy() {
        try {
            long count = quantumDataRepository.countByUsedFalseAndSource("NIST");

            if (count < 20) {
                log.info("📡 NIST BEACON: Reference pool low ({} units). Fetching latest public photonic pulse...", count);
                if (fetchAndSave()) {
                    log.info("✅ NIST BEACON: Pulse successfully captured and stored. New pool size: {}", count + 1);
                }
            } else {
                log.debug("📡 NIST BEACON: Pool sufficient ({} units). Skipping cycle.", count);
            }
        } catch (Exception e) {
            log.error("❌ NIST BEACON: Critical error in collection cycle: {}", e.getMessage());
        }
    }

    private boolean fetchAndSave() {
        try {
            String url = apiBaseUrl + "/api/v1/quantum-random?source=NIST&count=64&pure=true";
            log.info("📡 NIST BEACON: Requesting 512-bit pulse from internal gateway...");
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            String dataBase64 = root.path("data").asText();

            if (dataBase64 != null && !dataBase64.isEmpty()) {
                // We bypass local validation for the NIST Beacon because its 64-byte 
                // pulses are too small for the Shannon threshold (6.0), and the 
                // source is publicly trusted/verified by NIST.
                QuantumData quantumData = new QuantumData();
                quantumData.setDataBase64(dataBase64);
                quantumData.setUsed(false);
                quantumData.setSource("NIST");
                quantumDataRepository.save(quantumData);
                return true;
            } else {
                log.warn("⚠️ NIST BEACON: Received empty data from quantum-service.");
            }
        } catch (Exception e) {
            log.error("❌ NIST BEACON: API communication error: {}", e.getMessage());
        }
        return false;
    }
}
