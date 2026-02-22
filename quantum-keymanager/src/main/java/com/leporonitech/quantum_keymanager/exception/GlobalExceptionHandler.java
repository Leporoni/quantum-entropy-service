package com.leporonitech.quantum_keymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções para toda a aplicação.
 *
 * Intercepta exceções lançadas pelos controllers e as converte em
 * respostas HTTP padronizadas com mensagens amigáveis em JSON.
 *
 * Sem este handler, exceções não tratadas resultariam em respostas
 * genéricas do Spring (HTML de erro ou stack traces expostos).
 *
 * Anotação:
 * - @ControllerAdvice: Indica que esta classe intercepta exceções de todos os controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções de entropia insuficiente (InsufficientEntropyException).
     *
     * Retorna HTTP 422 (Unprocessable Entity) com um JSON contendo:
     * - error: tipo do erro ("InsufficientEntropy")
     * - message: mensagem descritiva com a quantidade disponível
     *
     * O frontend usa o status 422 para exibir o alerta de "Quantum Fuel Depleted".
     *
     * @param ex exceção de entropia insuficiente lançada pelo serviço
     * @return ResponseEntity com status 422 e corpo JSON com detalhes do erro
     */
    @ExceptionHandler(InsufficientEntropyException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientEntropy(InsufficientEntropyException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "InsufficientEntropy");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
}
