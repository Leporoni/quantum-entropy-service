package com.leporonitech.quantum_keymanager.validation;

/**
 * Validates entropy using the Monte Carlo method to estimate Pi (π).
 * Uses pairs of bytes as (x, y) coordinates in a 256x256 grid.
 * 
 * The ratio of points falling inside a circle inscribed in a square 
 * should approach π/4 for truly random data.
 */
public class MonteCarloValidator implements EntropyValidator {
    
    private String failureReason;
    private double piEstimate;
    private double errorPercentage;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 512) {
            failureReason = "Data too small for Monte Carlo test (minimum 512 bytes)";
            return false;
        }
        
        piEstimate = estimatePi(data);
        errorPercentage = Math.abs(piEstimate - Math.PI) / Math.PI * 100;
        
        // For small samples like 2048 bytes, 5-10% error is normal.
        // We'll use 15% as a safe upper bound for random-looking data.
        if (errorPercentage > 15.0) {
            failureReason = String.format("Monte Carlo Pi test failed (Estimate: %.4f, Error: %.2f%%)", 
                piEstimate, errorPercentage);
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
        return "MonteCarloValidator";
    }
    
    public double getPiEstimate() {
        return piEstimate;
    }
    
    public double getErrorPercentage() {
        return errorPercentage;
    }
    
    private double estimatePi(byte[] data) {
        int inCircle = 0;
        int totalPairs = data.length / 2;
        
        for (int i = 0; i < totalPairs; i++) {
            // Normalize bytes (-128 to 127) to [0, 1]
            double x = (double) (data[i * 2] & 0xFF) / 255.0;
            double y = (double) (data[i * 2 + 1] & 0xFF) / 255.0;
            
            // Shift to center for circle test: (x-0.5)^2 + (y-0.5)^2 <= 0.25
            if (Math.pow(x - 0.5, 2) + Math.pow(y - 0.5, 2) <= 0.25) {
                inCircle++;
            }
        }
        
        return 4.0 * inCircle / totalPairs;
    }
}