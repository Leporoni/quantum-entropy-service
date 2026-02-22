package com.leporonitech.quantum_random_service.controller;

import com.leporonitech.quantum_random_service.service.QuantumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuantumController {

    private final QuantumService quantumService;

    @GetMapping("/quantum-random")
    public ResponseEntity<Map<String, String>> getQuantumData(
            @RequestParam(defaultValue = "128") int count) {

        String base64Data = quantumService.getQuantumNumbersAsBase64(count);
        Map<String, String> response = Map.of("data", base64Data);
        return ResponseEntity.ok(response);
    }
}
