package com.leporonitech.quantum_keymanager.controller;

import com.leporonitech.quantum_keymanager.exception.InsufficientEntropyException;
import com.leporonitech.quantum_keymanager.model.RsaKey;
import com.leporonitech.quantum_keymanager.service.KeyManagerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class KeyManagerController {

    private final KeyManagerService keyManagerService;

    @PostMapping
    public ResponseEntity<RsaKeyResponse> createKey(@RequestBody CreateKeyRequest request) throws Exception {
        try {
            RsaKey key = keyManagerService.createRsaKey(request.getAlias(), request.getKeySize());
            return ResponseEntity.ok(new RsaKeyResponse(key));
        } catch (InsufficientEntropyException e) {
            throw e; // Rethrow to GlobalExceptionHandler
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<RsaKey>> listKeys() {
        return ResponseEntity.ok(keyManagerService.getAllKeys());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKey(@PathVariable Long id) {
        try {
            keyManagerService.deleteKey(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllKeys() {
        try {
            keyManagerService.deleteAllKeys();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<KeyManagerService.KeyExportResponse> exportKey(@PathVariable Long id) throws Exception {
        try {
            return ResponseEntity.ok(keyManagerService.exportPrivateKey(id));
        } catch (InsufficientEntropyException e) {
            throw e; // Rethrow to GlobalExceptionHandler
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Data
    public static class CreateKeyRequest {
        private String alias;
        private int keySize = 2048;
    }

    @Data
    public static class RsaKeyResponse {
        private Long id;
        private String alias;
        private String publicKey;
        private int keySize;
        private String createdAt;

        public RsaKeyResponse(RsaKey key) {
            this.id = key.getId();
            this.alias = key.getAlias();
            this.publicKey = key.getPublicKey();
            this.keySize = key.getKeySize();
            this.createdAt = key.getCreatedAt().toString();
        }
    }
}
