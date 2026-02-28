package com.leporonitech.quantum_keymanager.validation;

/**
 * Strategy interface for entropy validation following NIST SP 800-90B recommendations.
 * Each validator implements a specific validation test.
 */
public interface EntropyValidator {
    
    /**
     * Validates the given entropy data.
     *
     * @param data the raw bytes to validate
     * @return true if validation passes, false otherwise
     */
    boolean validate(byte[] data);
    
    /**
     * Returns the reason for validation failure.
     * Should be called only after validate() returns false.
     *
     * @return human-readable failure reason
     */
    String getFailureReason();
    
    /**
     * Returns the name of this validator for logging purposes.
     *
     * @return validator name
     */
    String getName();
}