package com.leporonitech.quantum_keymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando não há entropia quântica suficiente para realizar uma operação.
 *
 * Esta exceção é disparada em dois cenários:
 * 1. Geração de chave RSA: requer no mínimo 5 unidades de entropia.
 * 2. Exportação de chave privada: requer no mínimo 2 unidades de entropia.
 *
 * É tratada pelo GlobalExceptionHandler que retorna HTTP 422 (Unprocessable Entity)
 * com uma mensagem descritiva informando a quantidade disponível.
 *
 * Anotação:
 * - @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY): Define o status HTTP 422 como padrão
 *   caso a exceção não seja interceptada por um handler específico.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientEntropyException extends RuntimeException {

    /**
     * Cria uma nova exceção de entropia insuficiente.
     *
     * @param message mensagem descritiva (ex: "Insufficient quantum entropy. Current available: 3")
     */
    public InsufficientEntropyException(String message) {
        super(message);
    }
}
