package com.leporonitech.quantum_random_service.client;

import com.leporonitech.quantum_random_service.dto.LfdApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lfd-quantum-api", url = "https://lfdr.de/qrng_api")
public interface LfdQuantumApiClient {

    @GetMapping("/qrng")
    LfdApiResponse getRandomNumbers(
            @RequestParam("length") int length,
            @RequestParam("format") String format
    );
}
