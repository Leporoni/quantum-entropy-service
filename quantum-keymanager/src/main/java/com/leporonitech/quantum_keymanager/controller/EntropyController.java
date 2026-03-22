package com.leporonitech.quantum_keymanager.controller;

import com.leporonitech.quantum_keymanager.service.EntropyAuditService;
import com.leporonitech.quantum_keymanager.service.KeyManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quantum-entropy")
@RequiredArgsConstructor
public class EntropyController {

    private final KeyManagerService keyManagerService;
    private final EntropyAuditService entropyAuditService;

    @GetMapping("/status")
    public ResponseEntity<KeyManagerService.EntropyStatus> getStatus() {
        return ResponseEntity.ok(keyManagerService.getEntropyStatus());
    }

    @GetMapping("/audit")
    public ResponseEntity<EntropyAuditService.EntropyAuditReport> auditEntropy(
            @RequestParam(defaultValue = "2048") int size) {
        return ResponseEntity.ok(entropyAuditService.runFullAudit(size));
    }
}
