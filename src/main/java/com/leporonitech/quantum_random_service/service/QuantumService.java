package com.leporonitech.quantum_random_service.service;

import com.leporonitech.quantum_random_service.client.LfdQuantumApiClient;
import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HexFormat;

/**
 * Serviço responsável por buscar números aleatórios quânticos da API externa LfD
 * e convertê-los de formato hexadecimal para Base64.
 *
 * Fluxo de conversão:
 *   API LfD (HEX string) → byte[] → Base64 string
 *
 * Este serviço é consumido pelo QuantumController e, indiretamente, pelo
 * EntropyCollectorScheduler do quantum-keymanager (que chama a API REST).
 *
 * Anotações:
 * - @Service: Marca esta classe como um bean de serviço gerenciado pelo Spring.
 * - @RequiredArgsConstructor: Gera construtor para injeção do LfdQuantumApiClient.
 * - @Slf4j: Gera automaticamente o logger (log) via Lombok.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuantumService {

    /** Cliente Feign para comunicação com a API externa LfD QRNG. */
    private final LfdQuantumApiClient lfdQuantumApiClient;

    /**
     * Busca bytes aleatórios quânticos da API LfD e retorna como string Base64.
     *
     * Etapas:
     * 1. Valida o parâmetro 'count' (deve estar entre 1 e 1024).
     * 2. Chama a API LfD solicitando 'count' bytes em formato HEX.
     * 3. Converte a string hexadecimal recebida em um array de bytes.
     * 4. Codifica o array de bytes em Base64 e retorna.
     *
     * @param count quantidade de bytes aleatórios desejados (entre 1 e 1024)
     * @return string Base64 contendo os bytes aleatórios quânticos
     * @throws IllegalArgumentException se count estiver fora do intervalo permitido
     * @throws RuntimeException se a API externa falhar ou retornar dados inválidos
     */
    public String getQuantumNumbersAsBase64(int count) {
        // Validação do parâmetro de entrada
        if (count <= 0 || count > 1024) {
            log.warn("Valor de 'count' inválido: {}. Deve estar entre 1 e 1024.", count);
            throw new IllegalArgumentException("Count must be between 1 and 1024.");
        }

        // Busca os bytes aleatórios da API externa em formato hexadecimal
        log.info("Buscando {} bytes aleatórios quânticos da API LfD.", count);
        LfdApiResponse response = lfdQuantumApiClient.getRandomNumbers(count, "HEX");

        // Validação da resposta da API
        if (response == null || response.getQrn() == null) {
            log.error("Falha ao buscar números aleatórios quânticos. Resposta: {}", response);
            throw new RuntimeException("Failed to fetch quantum random numbers from LfD API.");
        }

        String hexString = response.getQrn();
        log.debug("String hexadecimal bruta recebida da API LfD: {}", hexString);

        // Converte a string hexadecimal para array de bytes
        byte[] bytes;
        try {
            bytes = HexFormat.of().parseHex(hexString);
        } catch (IllegalArgumentException e) {
             log.error("Falha ao parsear string hexadecimal da API LfD: {}", hexString, e);
             throw new RuntimeException("Invalid Hex string received from LfD API.", e);
        }

        // Codifica os bytes em Base64 para transporte seguro
        String base64String = Base64.getEncoder().encodeToString(bytes);
        log.info("String Base64 gerada com sucesso: {}", base64String);

        return base64String;
    }
}
