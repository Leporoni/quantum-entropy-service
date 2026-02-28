package com.leporonitech.quantum_keymanager.validation;

/**
 * Validates entropy using Shannon Entropy calculation.
 * NIST SP 800-90B recommends entropy assessment of random data.
 * 
 * Threshold: 6.0 bits/byte (max is 8.0)
 * A perfect distribution of 128 unique bytes yields ~7.0 entropy.
 * Random collisions reduce this further, so 6.0 is a safe lower bound.
 */
public class ShannonEntropyValidator implements EntropyValidator {
    
    private static final double MINIMUM_ENTROPY = 6.0;
    
    private String failureReason;
    private double calculatedEntropy;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length == 0) {
            failureReason = "Data is null or empty";
            return false;
        }
        
        calculatedEntropy = calculateShannonEntropy(data);
        
        if (calculatedEntropy < MINIMUM_ENTROPY) {
            failureReason = String.format("Low Shannon Entropy (%.2f bits/byte, minimum: %.1f)", 
                calculatedEntropy, MINIMUM_ENTROPY);
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getFailureReason() {
        return failureReason;
    }
    
    @Override
    public String getName() {
        return "ShannonEntropyValidator";
    }
    
    /**
     * Returns the calculated entropy value.
     * Useful for logging and debugging.
     *
     * @return calculated Shannon entropy in bits/byte
     */
    public double getCalculatedEntropy() {
        return calculatedEntropy;
    }
    
    /**
     * Calculates Shannon entropy of the given data.
     *
     * @param data the byte array to analyze
     * @return entropy value in bits/byte
     */
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
}