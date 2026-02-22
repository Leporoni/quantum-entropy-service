package com.leporonitech.quantum_random_service.controller;

import com.leporonitech.quantum_random_service.service.QuantumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller REST que expõe o endpoint para obter dados aleatórios quânticos.
 *
 * Este controller é a porta de entrada da API. Recebe requisições HTTP GET
 * e delega ao QuantumService a busca e conversão dos dados quânticos.
 *
 * Endpoint disponível:
 *   GET /api/v1/quantum-random?count={n}
 *
 * Anotações:
 * - @RestController: Indica que esta classe é um controller REST (respostas em JSON por padrão).
 * - @RequestMapping("/api/v1"): Define o prefixo base para todos os endpoints desta classe.
 * - @RequiredArgsConstructor: Gera automaticamente o construtor com os campos final (injeção de dependência via Lombok).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuantumController {

    /** Serviço responsável pela lógica de busca e conversão dos dados quânticos. */
    private final QuantumService quantumService;

    /**
     * Endpoint para obter dados aleatórios quânticos em formato Base64.
     *
     * Fluxo:
     * 1. Recebe o parâmetro 'count' (quantidade de bytes, padrão: 128).
     * 2. Delega ao QuantumService para buscar da API externa e converter para Base64.
     * 3. Retorna um JSON com a chave "data" contendo a string Base64.
     *
     * Exemplo de resposta:
     *   { "data": "SGVsbG8gV29ybGQ=..." }
     *
     * @param count quantidade de bytes aleatórios desejados (padrão: 128, máximo: 1024)
     * @return ResponseEntity com um Map contendo a string Base64 dos dados quânticos
     */
    @GetMapping("/quantum-random")
    public ResponseEntity<Map<String, String>> getQuantumData(
            @RequestParam(defaultValue = "128") int count) {

        String base64Data = quantumService.getQuantumNumbersAsBase64(count);
        Map<String, String> response = Map.of("data", base64Data);
        return ResponseEntity.ok(response);
    }
}
