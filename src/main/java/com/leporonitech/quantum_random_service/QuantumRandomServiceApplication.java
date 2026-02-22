package com.leporonitech.quantum_random_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class QuantumRandomServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuantumRandomServiceApplication.class, args);
	}

}
