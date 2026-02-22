package com.leporonitech.quantum_keymanager.controller;

import com.leporonitech.quantum_keymanager.exception.InsufficientEntropyException;
import com.leporonitech.quantum_keymanager.model.RsaKey;
import com.leporonitech.quantum_keymanager.service.KeyManagerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST principal para gerenciamento de chaves RSA.
 *
 * Expõe os endpoints CRUD para criar, listar, deletar e exportar
 * chaves RSA geradas com entropia quântica.
 *
 * Endpoints disponíveis:
 *   POST   /api/v1/keys          → Gerar nova chave RSA
 *   GET    /api/v1/keys          → Listar todas as chaves
 *   DELETE /api/v1/keys/{id}     → Deletar uma chave específica
 *   DELETE /api/v1/keys          → Deletar todas as chaves (limpar cofre)
 *   POST   /api/v1/keys/{id}/export → Exportar chave privada de forma segura
 */
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class KeyManagerController {

    /** Serviço que contém toda a lógica de negócio para gerenciamento de chaves. */
    private final KeyManagerService keyManagerService;

    /**
     * Gera um novo par de chaves RSA usando entropia quântica.
     *
     * Fluxo:
     * 1. Recebe alias (nome) e tamanho da chave no corpo da requisição.
     * 2. Delega ao KeyManagerService que consome entropia e gera o par RSA.
     * 3. Retorna os dados da chave criada (sem a chave privada em texto claro).
     *
     * Consome 5 unidades de entropia quântica.
     * Lança InsufficientEntropyException (HTTP 422) se não houver entropia suficiente.
     *
     * @param request corpo da requisição com alias e keySize
     * @return RsaKeyResponse com id, alias, chave pública, tamanho e data de criação
     */
    @PostMapping
    public ResponseEntity<RsaKeyResponse> createKey(@RequestBody CreateKeyRequest request) throws Exception {
        try {
            RsaKey key = keyManagerService.createRsaKey(request.getAlias(), request.getKeySize());
            return ResponseEntity.ok(new RsaKeyResponse(key));
        } catch (InsufficientEntropyException e) {
            throw e; // Relança para o GlobalExceptionHandler tratar com HTTP 422
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista todas as chaves RSA armazenadas no sistema.
     *
     * Retorna os dados públicos de cada chave (id, alias, chave pública, tamanho, data).
     * A chave privada criptografada NÃO é incluída nesta listagem.
     *
     * @return lista de RsaKey com todas as chaves do cofre
     */
    @GetMapping
    public ResponseEntity<List<RsaKey>> listKeys() {
        return ResponseEntity.ok(keyManagerService.getAllKeys());
    }

    /**
     * Deleta uma chave específica pelo seu ID.
     *
     * ATENÇÃO: Esta operação é irreversível. A chave privada será perdida permanentemente.
     *
     * @param id identificador único da chave a ser deletada
     * @return HTTP 204 (No Content) em caso de sucesso, ou HTTP 404 se não encontrada
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKey(@PathVariable Long id) {
        try {
            keyManagerService.deleteKey(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deleta TODAS as chaves do cofre (purge).
     *
     * ATENÇÃO: Esta operação é irreversível e destrói todas as chaves armazenadas.
     * Usada pelo botão "Clear Vault" no frontend.
     *
     * @return HTTP 204 (No Content) em caso de sucesso
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllKeys() {
        try {
            keyManagerService.deleteAllKeys();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Exporta a chave privada de forma segura usando o protocolo de Key Wrapping.
     *
     * Fluxo de exportação segura:
     * 1. Descriptografa a chave privada armazenada (que está protegida pela Master Key).
     * 2. Gera uma chave de transporte temporária (AES-256) usando entropia quântica fresca.
     * 3. Criptografa a chave privada com a chave de transporte.
     * 4. Retorna ambas (chave criptografada + chave de transporte) para o cliente.
     * 5. O cliente (frontend) descriptografa localmente no navegador.
     *
     * Consome 2 unidades de entropia quântica.
     * Lança InsufficientEntropyException (HTTP 422) se não houver entropia suficiente.
     *
     * @param id identificador da chave a ser exportada
     * @return KeyExportResponse com a chave criptografada, chave de transporte e algoritmo
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<KeyManagerService.KeyExportResponse> exportKey(@PathVariable Long id) throws Exception {
        try {
            return ResponseEntity.ok(keyManagerService.exportPrivateKey(id));
        } catch (InsufficientEntropyException e) {
            throw e; // Relança para o GlobalExceptionHandler tratar com HTTP 422
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DTO de entrada para criação de chaves RSA.
     *
     * Campos:
     * - alias: nome identificador da chave (ex: "minha-chave-segura")
     * - keySize: tamanho da chave em bits (padrão: 2048, opção: 4096)
     */
    @Data
    public static class CreateKeyRequest {
        private String alias;
        private int keySize = 2048;
    }

    /**
     * DTO de resposta para criação de chaves RSA.
     *
     * Contém apenas os dados públicos da chave criada.
     * A chave privada NUNCA é retornada neste DTO — apenas via exportação segura.
     */
    @Data
    public static class RsaKeyResponse {
        private Long id;
        private String alias;
        private String publicKey;
        private int keySize;
        private String createdAt;

        /**
         * Constrói o DTO de resposta a partir da entidade RsaKey.
         *
         * @param key entidade RsaKey salva no banco de dados
         */
        public RsaKeyResponse(RsaKey key) {
            this.id = key.getId();
            this.alias = key.getAlias();
            this.publicKey = key.getPublicKey();
            this.keySize = key.getKeySize();
            this.createdAt = key.getCreatedAt().toString();
        }
    }
}
