package com.leporonitech.quantum_keymanager.validation;

import java.util.zip.Deflater;

/**
 * Validates entropy by testing compressibility.
 * Random data should NOT be compressible.
 * 
 * If data compresses to less than 80% of original size,
 * it indicates patterns and is not truly random.
 */
public class CompressionValidator implements EntropyValidator {
    
    private static final double MAX_COMPRESSION_RATIO = 0.8;
    
    private String failureReason;
    private double compressionRatio;
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length == 0) {
            failureReason = "Data is null or empty";
            return false;
        }
        
        int compressedSize = compressData(data);
        compressionRatio = (double) compressedSize / data.length;
        
        // If compressed size is < 80% of original, it is compressible => Not random
        // Real random data usually expands when compressed due to overhead.
        if (compressedSize < data.length * MAX_COMPRESSION_RATIO) {
            failureReason = String.format("Data is compressible (pattern detected). Compression ratio: %.2f%%", 
                compressionRatio * 100);
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
        return "CompressionValidator";
    }
    
    /**
     * Returns the compression ratio achieved.
     *
     * @return compression ratio (compressed/original)
     */
    public double getCompressionRatio() {
        return compressionRatio;
    }
    
    /**
     * Attempts to compress the data using Deflate algorithm.
     *
     * @param data the byte array to compress
     * @return size of compressed data
     */
    private int compressData(byte[] data) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(data);
            deflater.finish();
            
            byte[] buffer = new byte[data.length + 100];
            int compressedSize = deflater.deflate(buffer);
            deflater.end();
            
            return compressedSize;
        } catch (Exception e) {
            // If compression fails, assume data is not compressible
            return data.length;
        }
    }
}