package com.leporonitech.quantum_random_service.client;

import com.leporonitech.quantum_random_service.dto.AnuApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "anu-quantum-api", url = "https://qrng.anu.edu.au")
public interface AnuQuantumApiClient {

    @GetMapping("/API/jsonI.php")
    AnuApiResponse getRandomNumbers(
            @RequestParam("length") int length,
            @RequestParam("type") String type
    );
}
