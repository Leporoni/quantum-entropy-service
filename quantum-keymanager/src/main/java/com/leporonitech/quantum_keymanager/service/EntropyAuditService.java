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
    }

    @Data
    @Builder
    public static class EntropyAuditReport {
        private int sampleSize;
        private List<AuditMetrics> results;
    }

    public EntropyAuditReport runFullAudit(int requestedSize) {
        log.info("Requested entropy audit for sample size: {}", requestedSize);
        
        byte[] quantumSample = getQuantumSample(requestedSize);
        int realSampleSize = quantumSample.length;
        
        log.info("Actual sample size used for audit: {} bytes", realSampleSize);
        
        List<AuditMetrics> results = new ArrayList<>();
        
        if (realSampleSize > 0) {
            results.add(auditSource("Quantum (LFD)", quantumSample));
            results.add(auditSource("Java SecureRandom (CSPRNG)", getCsprngSample(realSampleSize)));
            results.add(auditSource("Java Random (LCRNG)", getPrngSample(realSampleSize)));
        }
        
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

        return AuditMetrics.builder()
                .source(name)
                .shannonEntropy(shannon.getCalculatedEntropy())
                .chiSquare(chiSquare.getChiSquareValue())
                .piEstimate(monteCarlo.getPiEstimate())
                .compressionRatio(compression.getCompressionRatio())
                .repetitions(repetition.getRepetitionCount())
                .base64Sample(Base64.getEncoder().encodeToString(data))
                .build();
    }

    private byte[] getQuantumSample(int size) {
        // Just take the latest unused data without locking/consuming it
        // 32.768 bytes / 32 bytes/record = 1024 records. Using slightly more to be safe.
        List<QuantumData> data = quantumDataRepository.findAll(PageRequest.of(0, (size / 32) + 100)).getContent();
        
        // Calculate max available bytes
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
        
        // Final sanity check: if we somehow have fewer bytes than calculated
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
}