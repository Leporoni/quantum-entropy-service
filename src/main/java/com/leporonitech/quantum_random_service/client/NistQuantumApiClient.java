package com.leporonitech.quantum_random_service.client;

import com.leporonitech.quantum_random_service.dto.NistApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "nist-beacon-api", url = "https://beacon.nist.gov")
public interface NistQuantumApiClient {

    @GetMapping("/beacon/2.0/pulse/last")
    NistApiResponse getLastPulse();
}
