package com.leporonitech.quantum_keymanager.controller;

import com.leporonitech.quantum_keymanager.service.KeyManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST que expõe o status da entropia quântica disponível no sistema.
 *
 * Este endpoint é consumido pelo componente EntropyMeter do frontend
 * para exibir o medidor de "Quantum Fuel" em tempo real.
 *
 * Endpoint disponível:
 *   GET /api/v1/quantum-entropy/status
 *
 * Retorna:
 *   - availableRecords: quantidade de registros de entropia não utilizados
 *   - costPerGeneration: custo em unidades para gerar uma chave (5)
 *   - costPerExport: custo em unidades para exportar uma chave (2)
 */
@RestController
@RequestMapping("/api/v1/quantum-entropy")
@RequiredArgsConstructor
public class EntropyController {

    /** Serviço de gerenciamento de chaves que também fornece o status da entropia. */
    private final KeyManagerService keyManagerService;

    /**
     * Retorna o status atual da entropia quântica disponível.
     *
     * O frontend faz polling neste endpoint a cada 5 segundos para
     * atualizar o medidor de entropia na interface.
     *
     * @return EntropyStatus com a quantidade disponível e os custos por operação
     */
    @GetMapping("/status")
    public ResponseEntity<KeyManagerService.EntropyStatus> getStatus() {
        return ResponseEntity.ok(keyManagerService.getEntropyStatus());
    }
}
