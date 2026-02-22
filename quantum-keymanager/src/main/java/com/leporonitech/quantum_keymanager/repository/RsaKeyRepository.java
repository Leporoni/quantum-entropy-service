package com.leporonitech.quantum_keymanager.repository;

import com.leporonitech.quantum_keymanager.model.RsaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para acesso às chaves RSA armazenadas no cofre (tabela rsa_keys).
 *
 * Estende JpaRepository que já fornece CRUD básico:
 * - save(): salvar/atualizar chave
 * - findAll(): listar todas as chaves
 * - findById(): buscar por ID
 * - deleteById(): deletar por ID
 * - deleteAll(): limpar todo o cofre
 * - existsById(): verificar existência
 *
 * Anotação:
 * - @Repository: Marca como componente de acesso a dados do Spring.
 */
@Repository
public interface RsaKeyRepository extends JpaRepository<RsaKey, Long> {

    /**
     * Busca uma chave RSA pelo seu alias (nome identificador).
     *
     * Retorna Optional para tratamento seguro de chave não encontrada.
     * O alias é único na tabela (constraint UNIQUE), então retorna no máximo um resultado.
     *
     * @param alias nome identificador da chave
     * @return Optional contendo a chave se encontrada, ou vazio se não existir
     */
    Optional<RsaKey> findByAlias(String alias);
}
