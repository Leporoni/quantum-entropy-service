package com.leporonitech.quantum_keymanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um registro de entropia quântica armazenado no banco de dados.
 *
 * Cada registro contém um bloco de bytes aleatórios quânticos (em Base64) coletados
 * da API Quantum Random Service pelo EntropyCollectorScheduler.
 *
 * Ciclo de vida de um registro:
 * 1. CRIAÇÃO: O scheduler coleta dados da API e salva com used=false.
 * 2. CONSUMO: Ao gerar/exportar uma chave, os registros são marcados como used=true.
 * 3. Os registros usados permanecem no banco como histórico (não são deletados).
 *
 * Tabela: quantum_data
 *
 * Anotações:
 * - @Entity: Marca como entidade JPA (mapeada para tabela no banco).
 * - @Table: Define o nome da tabela no banco de dados.
 * - @Data (Lombok): Gera getters, setters, toString, equals e hashCode.
 */
@Entity
@Table(name = "quantum_data")
@Data
public class QuantumData {

    /** Identificador único do registro (auto-incremento). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dados aleatórios quânticos codificados em Base64 (tipicamente 128 bytes). */
    @Column(name = "data_base64", nullable = false)
    private String dataBase64;

    /** Data/hora de criação do registro (preenchida automaticamente pelo banco). */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Flag que indica se este registro já foi consumido.
     * Segue o padrão "Consume & Discard": uma vez usado, não pode ser reutilizado.
     * Isso garante que a mesma entropia nunca seja usada duas vezes (segurança criptográfica).
     */
    @Column(name = "used", nullable = false)
    private boolean used = false;
}
