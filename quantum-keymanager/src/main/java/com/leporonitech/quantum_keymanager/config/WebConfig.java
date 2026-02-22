package com.leporonitech.quantum_keymanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS (Cross-Origin Resource Sharing) para a API do Key Manager.
 *
 * Permite que o frontend (quantum-keymanager-ui) faça requisições à API
 * mesmo estando em uma origem diferente (ex: localhost:3000 → localhost:8082).
 *
 * Sem esta configuração, o navegador bloquearia as requisições do frontend
 * por política de segurança de mesma origem (Same-Origin Policy).
 *
 * Anotação:
 * - @Configuration: Indica que esta classe contém definições de beans do Spring.
 */
@Configuration
public class WebConfig {

    /**
     * Configura as regras de CORS para todos os endpoints da API (/api/**).
     *
     * Regras definidas:
     * - allowedOrigins("*"): Aceita requisições de qualquer origem (em produção,
     *   recomenda-se restringir para o domínio específico do frontend).
     * - allowedMethods: Permite GET, POST, PUT, DELETE e OPTIONS (preflight).
     * - allowedHeaders("*"): Aceita qualquer cabeçalho na requisição.
     *
     * @return WebMvcConfigurer com as regras de CORS configuradas
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
