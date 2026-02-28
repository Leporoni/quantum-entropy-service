package com.leporonitech.quantum_keymanager.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Composite validator that runs all entropy validators in sequence.
 * Follows the Composite Pattern to treat individual validators uniformly.
 * 
 * If any validator fails, the entire validation fails.
 * Logs detailed information about which validator failed and why.
 */
@Component
@Slf4j
public class EntropyValidatorComposite implements EntropyValidator {
    
    private final List<EntropyValidator> validators;
    private String failureReason;
    private String failedValidatorName;
    
    /**
     * Creates a composite with default validators.
     */
    public EntropyValidatorComposite() {
        this.validators = Arrays.asList(
            new DataSizeValidator(),
            new RepetitionValidator(),
            new ShannonEntropyValidator(),
            new CompressionValidator()
        );
    }
    
    /**
     * Creates a composite with custom validators.
     * Useful for testing or custom validation configurations.
     *
     * @param validators list of validators to use
     */
    public EntropyValidatorComposite(List<EntropyValidator> validators) {
        this.validators = new ArrayList<>(validators);
    }
    
    @Override
    public boolean validate(byte[] data) {
        for (EntropyValidator validator : validators) {
            if (!validator.validate(data)) {
                failedValidatorName = validator.getName();
                failureReason = validator.getFailureReason();
                log.warn("Validation Failed [{}]: {}", failedValidatorName, failureReason);
                return false;
            }
        }
        
        log.debug("All {} validators passed successfully", validators.size());
        return true;
    }
    
    @Override
    public String getFailureReason() {
        if (failedValidatorName != null && failureReason != null) {
            return String.format("[%s] %s", failedValidatorName, failureReason);
        }
        return failureReason;
    }
    
    @Override
    public String getName() {
        return "EntropyValidatorComposite";
    }
    
    /**
     * Returns the name of the validator that failed.
     *
     * @return name of failed validator, or null if validation passed
     */
    public String getFailedValidatorName() {
        return failedValidatorName;
    }
    
    /**
     * Returns the list of validators in this composite.
     *
     * @return list of validators
     */
    public List<EntropyValidator> getValidators() {
        return new ArrayList<>(validators);
    }
}