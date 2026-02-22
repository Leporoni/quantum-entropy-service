package com.leporonitech.quantum_keymanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um par de chaves RSA armazenado no cofre (vault).
 *
 * Cada registro contém:
 * - A chave pública em Base64 (visível para todos)
 * - A chave privada CRIPTOGRAFADA com a Master Key do sistema (AES)
 * - Metadados: alias, tamanho da chave e data de criação
 *
 * IMPORTANTE: A chave privada NUNCA é armazenada em texto claro.
 * Ela é criptografada com AES usando a Master Key definida na variável
 * de ambiente MASTER_KEY_SECRET antes de ser salva no banco.
 *
 * Tabela: rsa_keys
 */
@Entity
@Table(name = "rsa_keys")
@Data
public class RsaKey {

    /** Identificador único da chave (auto-incremento). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome identificador da chave definido pelo usuário (deve ser único). */
    @Column(nullable = false, unique = true)
    private String alias;

    /** Chave pública RSA codificada em Base64 (formato X.509 SubjectPublicKeyInfo). */
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;

    /**
     * Chave privada RSA criptografada com a Master Key (AES) e codificada em Base64.
     * Para acessar a chave privada original, é necessário usar o endpoint de exportação
     * que aplica o protocolo de Key Wrapping.
     */
    @Column(name = "private_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String privateKeyEncrypted;

    /** Tamanho da chave em bits (2048 ou 4096). */
    @Column(name = "key_size", nullable = false)
    private int keySize;

    /** Data/hora de criação da chave (preenchida automaticamente pelo @PrePersist). */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Callback JPA executado automaticamente antes de persistir a entidade.
     * Define a data de criação como o momento atual.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
