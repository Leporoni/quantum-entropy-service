package com.leporonitech.quantum_keymanager.service;

import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.validation.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntropyAuditService {

    private final QuantumDataRepository quantumDataRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Random prng = new Random();

    @Data
    @Builder
    public static class AuditMetrics {
        private String source;
        private double shannonEntropy;
        private double chiSquare;
        private double piEstimate;
        private double compressionRatio;
        private int repetitions;
        private String base64Sample;
        private String fingerprintHex; // Traceability fingerprint (first 16 bytes)
    }

    @Data
    @Builder
    public static class EntropyAuditReport {
        private int sampleSize;
        private List<AuditMetrics> results;
    }

    public EntropyAuditReport runFullAudit(int requestedSize) {
        log.info("Starting High-Resolution Entropy Audit. Requested: {} bytes.", requestedSize);
        
        byte[] quantumSample = getQuantumSample(requestedSize);
        int realSampleSize = quantumSample.length;
        
        log.info("Actual sample size used for audit: {} bytes", realSampleSize);
        
        List<AuditMetrics> results = new ArrayList<>();
        
        if (realSampleSize > 0) {
            results.add(auditSource("Quantum (LFD)", quantumSample));
            results.add(auditSource("Java SecureRandom (CSPRNG)", getCsprngSample(realSampleSize)));
            results.add(auditSource("Java Random (LCRNG)", getPrngSample(realSampleSize)));
        }
        
        log.info("Entropy Audit Completed successfully for {} sources.", results.size());
        return EntropyAuditReport.builder()
                .sampleSize(realSampleSize)
                .results(results)
                .build();
    }

    private AuditMetrics auditSource(String name, byte[] data) {
        ShannonEntropyValidator shannon = new ShannonEntropyValidator();
        ChiSquareValidator chiSquare = new ChiSquareValidator();
        MonteCarloValidator monteCarlo = new MonteCarloValidator();
        CompressionValidator compression = new CompressionValidator();
        RepetitionValidator repetition = new RepetitionValidator();

        shannon.validate(data);
        chiSquare.validate(data);
        monteCarlo.validate(data);
        compression.validate(data);
        repetition.validate(data);

        // Generate fingerprint for traceability (first 16 bytes)
        String fingerprint = bytesToHex(Arrays.copyOf(data, Math.min(data.length, 16)));
        log.info("AUDIT TRACEABILITY - Source: [{}], Fingerprint (First 16 bytes): [{}]", name, fingerprint);

        return AuditMetrics.builder()
                .source(name)
                .shannonEntropy(shannon.getCalculatedEntropy())
                .chiSquare(chiSquare.getChiSquareValue())
                .piEstimate(monteCarlo.getPiEstimate())
                .compressionRatio(compression.getCompressionRatio())
                .repetitions(repetition.getRepetitionCount())
                .base64Sample(Base64.getEncoder().encodeToString(data))
                .fingerprintHex(fingerprint)
                .build();
    }

    private byte[] getQuantumSample(int size) {
        // Fetch unused data from repository
        List<QuantumData> data = quantumDataRepository.findAll(PageRequest.of(0, (size / 32) + 100)).getContent();
        
        int maxAvailable = 0;
        for (QuantumData q : data) {
            maxAvailable += Base64.getDecoder().decode(q.getDataBase64()).length;
        }
        
        int actualSize = Math.min(size, maxAvailable);
        if (actualSize <= 0) return new byte[0];

        byte[] sample = new byte[actualSize];
        int pos = 0;
        for (QuantumData q : data) {
            byte[] bytes = Base64.getDecoder().decode(q.getDataBase64());
            int len = Math.min(bytes.length, actualSize - pos);
            if (len <= 0) break;
            System.arraycopy(bytes, 0, sample, pos, len);
            pos += len;
            if (pos >= actualSize) break;
        }
        
        if (pos < actualSize) {
            return Arrays.copyOf(sample, pos);
        }
        
        return sample;
    }

    private byte[] getCsprngSample(int size) {
        byte[] sample = new byte[size];
        secureRandom.nextBytes(sample);
        return sample;
    }

    private byte[] getPrngSample(int size) {
        byte[] sample = new byte[size];
        prng.nextBytes(sample);
        return sample;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}