package com.leporonitech.quantum_keymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Classe principal da aplicação Quantum Key Manager.
 *
 * Este microsserviço é responsável por:
 * - Coletar entropia quântica da API Quantum Random Service
 * - Gerar pares de chaves RSA (2048/4096 bits) usando entropia quântica como semente
 * - Armazenar as chaves com a chave privada criptografada por uma Master Key (AES)
 * - Exportar chaves privadas de forma segura usando Key Wrapping (chave de transporte AES-256)
 *
 * Anotações:
 * - @SpringBootApplication: Configuração automática do Spring Boot.
 * - @EnableScheduling: Habilita tarefas agendadas (@Scheduled), usado pelo
 *   EntropyCollectorScheduler para coletar entropia periodicamente.
 */
@SpringBootApplication
@EnableScheduling
public class QuantumKeymanagerApplication {

	/**
	 * Método main — ponto de entrada da JVM.
	 * Inicializa o contexto do Spring Boot e sobe o servidor embutido.
	 *
	 * @param args argumentos de linha de comando (opcionais)
	 */
	public static void main(String[] args) {
		SpringApplication.run(QuantumKeymanagerApplication.class, args);
	}

	/**
	 * Bean do RestTemplate — cliente HTTP usado pelo EntropyCollectorScheduler
	 * para buscar dados da API Quantum Random Service.
	 *
	 * Registrado como bean para permitir injeção de dependência em qualquer
	 * componente que precise fazer chamadas HTTP.
	 *
	 * @return instância do RestTemplate configurada com valores padrão
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
