package com.leporonitech.quantum_keymanager.validation;

/**
 * Validates that entropy data meets minimum size requirements.
 * NIST SP 800-90B recommends a minimum of 32 bytes for meaningful entropy.
 */
public class DataSizeValidator implements EntropyValidator {
    
    private static final int MINIMUM_SIZE_BYTES = 32;
    
    private String failureReason;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null) {
            failureReason = "Data is null";
            return false;
        }
        
        if (data.length < MINIMUM_SIZE_BYTES) {
            failureReason = String.format("Data too short (%d bytes, minimum: %d bytes)", 
                data.length, MINIMUM_SIZE_BYTES);
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
        return "DataSizeValidator";
    }
}