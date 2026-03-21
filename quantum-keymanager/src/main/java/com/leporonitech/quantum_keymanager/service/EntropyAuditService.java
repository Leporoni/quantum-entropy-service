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
import java.util.stream.Collectors;

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

    public EntropyAuditReport runFullAudit(int sampleSize) {
        log.info("Starting entropy audit for sample size: {}", sampleSize);
        
        List<AuditMetrics> results = new ArrayList<>();
        
        results.add(auditSource("Quantum (LFD)", getQuantumSample(sampleSize)));
        results.add(auditSource("Java SecureRandom (CSPRNG)", getCsprngSample(sampleSize)));
        results.add(auditSource("Java Random (LCRNG)", getPrngSample(sampleSize)));
        
        return EntropyAuditReport.builder()
                .sampleSize(sampleSize)
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
        List<QuantumData> data = quantumDataRepository.findAll(PageRequest.of(0, (size / 32) + 1)).getContent();
        
        byte[] sample = new byte[size];
        int pos = 0;
        for (QuantumData q : data) {
            byte[] bytes = Base64.getDecoder().decode(q.getData());
            int len = Math.min(bytes.length, size - pos);
            System.arraycopy(bytes, 0, sample, pos, len);
            pos += len;
            if (pos >= size) break;
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