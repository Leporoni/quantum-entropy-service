package com.leporonitech.quantum_random_service.client;

import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign para a API externa de números aleatórios quânticos da LfD (Leibniz University).
 *
 * O OpenFeign gera automaticamente a implementação desta interface em tempo de execução.
 * Cada método mapeado corresponde a uma chamada HTTP para a API externa.
 *
 * URL base: https://lfdr.de/qrng_api
 * Documentação da API: https://lfdr.de/qrng_api
 *
 * Anotações:
 * - @FeignClient: Define o nome lógico do cliente ("lfd-quantum-api") e a URL base da API externa.
 */
@FeignClient(name = "lfd-quantum-api", url = "https://lfdr.de/qrng_api")
public interface LfdQuantumApiClient {

    /**
     * Busca números aleatórios quânticos da API LfD.
     *
     * Faz uma requisição GET para /qrng com os parâmetros de quantidade e formato.
     * A API retorna os bytes aleatórios no formato solicitado (HEX neste caso).
     *
     * @param length quantidade de bytes aleatórios a serem gerados (ex: 128)
     * @param format formato de saída dos dados (usamos "HEX" — string hexadecimal)
     * @return LfdApiResponse contendo a string hexadecimal (qrn) e o tamanho (length)
     */
    @GetMapping("/qrng")
    LfdApiResponse getRandomNumbers(
            @RequestParam("length") int length,
            @RequestParam("format") String format
    );
}
