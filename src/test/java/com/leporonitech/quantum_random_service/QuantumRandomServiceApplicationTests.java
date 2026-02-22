package com.leporonitech.quantum_random_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de integração que verifica se o contexto do Spring Boot
 * é carregado corretamente sem erros.
 *
 * Este teste garante que:
 * - Todas as dependências estão configuradas corretamente
 * - Os beans são criados sem conflitos
 * - A aplicação consegue inicializar por completo
 *
 * Se este teste falhar, significa que há algum problema de configuração
 * (ex: bean faltando, propriedade obrigatória não definida, etc).
 */
@SpringBootTest
class QuantumRandomServiceApplicationTests {

	/**
	 * Testa se o contexto do Spring Boot carrega sem erros.
	 * Se a aplicação subir corretamente, o teste passa automaticamente.
	 */
	@Test
	void contextLoads() {
	}

}
