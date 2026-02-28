package com.leporonitech.quantum_keymanager.validation;

/**
 * Validates entropy by checking for excessive byte repetition.
 * Implements a simplified Monobit/Runs test as per NIST SP 800-90B.
 * 
 * Random data should not have excessive consecutive byte repetitions.
 * Threshold: Maximum 20% of bytes can be consecutive repetitions.
 */
public class RepetitionValidator implements EntropyValidator {
    
    private static final double MAX_REPETITION_RATIO = 0.2;
    
    private String failureReason;
    private int repetitionCount;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length == 0) {
            failureReason = "Data is null or empty";
            return false;
        }
        
        repetitionCount = countRepetitions(data);
        int maxAllowed = (int) (data.length * MAX_REPETITION_RATIO);
        
        if (repetitionCount > maxAllowed) {
            failureReason = String.format("Excessive repetition detected (%d repetitions, max allowed: %d)", 
                repetitionCount, maxAllowed);
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
        return "RepetitionValidator";
    }
    
    /**
     * Returns the count of consecutive repetitions found.
     *
     * @return number of consecutive byte repetitions
     */
    public int getRepetitionCount() {
        return repetitionCount;
    }
    
    /**
     * Counts consecutive byte repetitions in the data.
     *
     * @param data the byte array to analyze
     * @return count of consecutive repetitions
     */
    private int countRepetitions(byte[] data) {
        int repetitions = 0;
        for (int i = 1; i < data.length; i++) {
            if (data[i] == data[i - 1]) {
                repetitions++;
            }
        }
        return repetitions;
    }
}