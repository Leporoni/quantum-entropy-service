package com.leporonitech.quantum_random_service.dto;

import lombok.Data;

/**
 * DTO (Data Transfer Object) que representa a resposta da API externa LfD QRNG.
 *
 * A API LfD retorna um JSON com dois campos:
 * - qrn: string hexadecimal contendo os bytes aleatórios quânticos gerados
 * - length: quantidade de bytes que foram gerados
 *
 * Exemplo de resposta da API:
 *   { "qrn": "a3f2b1c4d5e6...", "length": 128 }
 *
 * Anotação:
 * - @Data (Lombok): Gera automaticamente getters, setters, toString, equals e hashCode.
 */
@Data
public class LfdApiResponse {

    /** String hexadecimal contendo os bytes aleatórios quânticos (Quantum Random Number). */
    private String qrn;

    /** Quantidade de bytes gerados pela API. */
    private int length;
}
