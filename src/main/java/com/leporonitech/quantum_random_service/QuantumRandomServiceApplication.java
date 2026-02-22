package com.leporonitech.quantum_random_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Classe principal da aplicação Quantum Random Service.
 *
 * Este é o ponto de entrada do microsserviço responsável por buscar
 * números aleatórios quânticos de uma API externa (LfD QRNG) e
 * expô-los como strings Base64 através de uma API REST.
 *
 * Anotações:
 * - @SpringBootApplication: Combina @Configuration, @EnableAutoConfiguration e @ComponentScan.
 *   Inicializa toda a configuração automática do Spring Boot.
 * - @EnableFeignClients: Habilita o uso do OpenFeign para chamadas HTTP declarativas.
 *   Permite que interfaces anotadas com @FeignClient sejam automaticamente implementadas
 *   pelo Spring como clientes HTTP.
 */
@EnableFeignClients
@SpringBootApplication
public class QuantumRandomServiceApplication {

	/**
	 * Método main — ponto de entrada da JVM.
	 * Inicializa o contexto do Spring Boot e sobe o servidor embutido (Tomcat).
	 *
	 * @param args argumentos de linha de comando (opcionais)
	 */
	public static void main(String[] args) {
		SpringApplication.run(QuantumRandomServiceApplication.class, args);
	}

}
