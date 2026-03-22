package com.leporonitech.quantum_keymanager.validation;

/**
 * Validates entropy using the Chi-Square (χ²) test for uniformity.
 * Checks if the frequency of each byte (0-255) follows a uniform distribution.
 * 
 * For N bytes, the expected frequency (Ei) for each byte is N/256.
 * χ² = Σ (Oi - Ei)² / Ei
 * 
 * A value close to 255 (degrees of freedom) indicates a good random distribution.
 */
public class ChiSquareValidator implements EntropyValidator {
    
    private String failureReason;
    private double chiSquareValue;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 256) {
            failureReason = "Data too small for Chi-Square test (minimum 256 bytes)";
            return false;
        }
        
        chiSquareValue = calculateChiSquare(data);
        
        // Critical values for 255 degrees of freedom:
        // 0.01% = 178.4 (too uniform - suspicious)
        // 99.9% = 345.9 (not uniform - non-random)
        if (chiSquareValue > 345.9 || chiSquareValue < 178.4) {
            failureReason = String.format("Chi-Square test failed (value: %.2f, expected range: 178-346)", 
                chiSquareValue);
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
        return "ChiSquareValidator";
    }
    
    public double getChiSquareValue() {
        return chiSquareValue;
    }
    
    private double calculateChiSquare(byte[] data) {
        int[] frequencies = new int[256];
        for (byte b : data) {
            frequencies[b & 0xFF]++;
        }
        
        double expected = (double) data.length / 256.0;
        double chiSquare = 0;
        
        for (int count : frequencies) {
            chiSquare += Math.pow(count - expected, 2) / expected;
        }
        
        return chiSquare;
    }
}