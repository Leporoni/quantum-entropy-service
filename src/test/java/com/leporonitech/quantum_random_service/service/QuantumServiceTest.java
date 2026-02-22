package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para o QuantumService.
 *
 * Utiliza Mockito para simular (mockar) o cliente Feign (LfdQuantumApiClient),
 * permitindo testar a lógica de conversão HEX → Base64 sem depender da API externa.
 *
 * Cenários testados:
 * 1. Requisição válida retorna Base64 correto
 * 2. Count acima do limite (>1024) lança exceção
 * 3. Count abaixo do limite (<=0) lança exceção
 * 4. Resposta nula da API lança exceção
 * 5. Campo QRN nulo na resposta lança exceção
 *
 * Anotações:
 * - @ExtendWith(MockitoExtension.class): Habilita o Mockito no JUnit 5.
 * - @Mock: Cria um mock do LfdQuantumApiClient.
 * - @InjectMocks: Cria uma instância real do QuantumService com o mock injetado.
 */
@ExtendWith(MockitoExtension.class)
class QuantumServiceTest {

    /** Mock do cliente Feign — simula chamadas à API externa sem fazer requisições reais. */
    @Mock
    private LfdQuantumApiClient lfdQuantumApiClient;

    /** Instância real do serviço com o mock injetado automaticamente. */
    @InjectMocks
    private QuantumService quantumService;

    /**
     * Testa o cenário feliz: requisição válida deve retornar a string Base64 correta.
     *
     * Dado: A API retorna "48656c6c6f" (que é "Hello" em hexadecimal)
     * Quando: Chamamos getQuantumNumbersAsBase64(5)
     * Então: O resultado deve ser "SGVsbG8=" (que é "Hello" em Base64)
     */
    @Test
    void shouldReturnBase64StringWhenRequestIsValid() {
        // Arrange (Preparação)
        String hexString = "48656c6c6f"; // "Hello" em hexadecimal
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(hexString);
        mockResponse.setLength(5);

        when(lfdQuantumApiClient.getRandomNumbers(5, "HEX")).thenReturn(mockResponse);

        // Act (Execução)
        String result = quantumService.getQuantumNumbersAsBase64(5);

        // Assert (Verificação)
        assertEquals("SGVsbG8=", result); // "Hello" em Base64
        verify(lfdQuantumApiClient).getRandomNumbers(5, "HEX");
    }

    /**
     * Testa que um count acima de 1024 lança IllegalArgumentException.
     * O limite máximo da API é 1024 bytes por requisição.
     */
    @Test
    void shouldThrowExceptionWhenCountIsTooHigh() {
        // Arrange (Preparação)
        int count = 1025;

        // Act & Assert (Execução e Verificação)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(count);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    /**
     * Testa que um count igual a zero lança IllegalArgumentException.
     * O mínimo permitido é 1 byte.
     */
    @Test
    void shouldThrowExceptionWhenCountIsTooLow() {
        // Arrange (Preparação)
        int count = 0;

        // Act & Assert (Execução e Verificação)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(count);
        });

        assertEquals("Count must be between 1 and 1024.", exception.getMessage());
    }

    /**
     * Testa que uma resposta nula da API externa lança RuntimeException.
     * Isso pode acontecer se a API estiver fora do ar ou retornar erro.
     */
    @Test
    void shouldThrowExceptionWhenApiResponseIsNull() {
        // Arrange (Preparação)
        when(lfdQuantumApiClient.getRandomNumbers(anyInt(), anyString())).thenReturn(null);

        // Act & Assert (Execução e Verificação)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(10);
        });

        assertEquals("Failed to fetch quantum random numbers from LfD API.", exception.getMessage());
    }

    /**
     * Testa que um campo QRN nulo na resposta lança RuntimeException.
     * A API pode retornar um JSON válido mas sem o campo de dados.
     */
    @Test
    void shouldThrowExceptionWhenQrnIsNull() {
        // Arrange (Preparação)
        LfdApiResponse mockResponse = new LfdApiResponse();
        mockResponse.setQrn(null);

        when(lfdQuantumApiClient.getRandomNumbers(10, "HEX")).thenReturn(mockResponse);

        // Act & Assert (Execução e Verificação)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quantumService.getQuantumNumbersAsBase64(10);
        });

        assertEquals("Failed to fetch quantum random numbers from LfD API.", exception.getMessage());
    }
}
