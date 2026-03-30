package com.leporonitech.quantum_random_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NistApiResponse {
    private Pulse pulse;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pulse {
        private String uri;
        private String outputValue; // O valor de 512 bits (64 bytes) gerado
        private String localOutputValue; // Valor local antes da combinação
        private String timeStamp;
    }
}
