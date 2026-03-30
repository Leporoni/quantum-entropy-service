package com.leporonitech.quantum_keymanager.service;

import com.leporonitech.quantum_keymanager.model.QuantumData;
import com.leporonitech.quantum_keymanager.repository.QuantumDataRepository;
import com.leporonitech.quantum_keymanager.validation.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
        private String fingerprintHex;
    }

    @Data
    @Builder
    public static class EntropyAuditReport {
        private int sampleSize;
        private List<AuditMetrics> results;
    }

    public EntropyAuditReport runFullAudit(int requestedSize) {
        log.info("Starting Dynamic Multi-Source Audit. Requested: {} bytes.", requestedSize);
        
        List<AuditMetrics> results = new ArrayList<>();
        int realSampleSize = 0;

        // 1. Audit LFD Quantum Source
        byte[] lfdSample = getQuantumSample("LFD", requestedSize);
        if (lfdSample.length > 0) {
            realSampleSize = lfdSample.length;
            results.add(auditSource("Quantum (LFD)", lfdSample));
        }

        // 2. Audit ANU Quantum Source
        byte[] anuSample = getQuantumSample("ANU", requestedSize);
        if (anuSample.length > 0) {
            // Use LFD size if available to keep comparison fair
            int sizeToAudit = realSampleSize > 0 ? realSampleSize : anuSample.length;
            if (anuSample.length > sizeToAudit) {
                anuSample = Arrays.copyOf(anuSample, sizeToAudit);
            }
            results.add(auditSource("Quantum (ANU)", anuSample));
            if (realSampleSize == 0) realSampleSize = anuSample.length;
        }

        // 2.5 Audit NIST Public Photonic Beacon
        byte[] nistSample = getQuantumSample("NIST", requestedSize);
        if (nistSample.length > 0) {
            results.add(auditSource("Quantum (NIST Beacon)", nistSample));
        }

        // 3. Audit Local PRNGs
        if (realSampleSize > 0) {
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

        String fingerprint = bytesToHex(Arrays.copyOf(data, Math.min(data.length, 16)));
        log.info("AUDIT TRACEABILITY - Source: [{}], Fingerprint: [{}]", name, fingerprint);

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

    private byte[] getQuantumSample(String source, int size) {
        long totalRecords = quantumDataRepository.countByUsedFalseAndSource(source);
        if (totalRecords == 0) return new byte[0];

        // Each record has 256 bytes. Calculate how many records we need.
        int recordsNeeded = (size / 256) + 1;
        
        // Calculate a random start page/offset to make the audit dynamic
        int maxStartOffset = (int) Math.max(0, totalRecords - recordsNeeded);
        int randomStart = maxStartOffset > 0 ? secureRandom.nextInt(maxStartOffset) : 0;
        
        log.info("Dynamic Sampling [{}]: Selecting {} records starting from offset {} (Total in pool: {})", 
                 source, recordsNeeded, randomStart, totalRecords);

        // Fetch the records starting from random offset
        Page<QuantumData> dataPage = quantumDataRepository.findByUsedFalseAndSource(
                source, 
                PageRequest.of(randomStart / recordsNeeded, recordsNeeded)
        );
        List<QuantumData> data = dataPage.getContent();
        
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