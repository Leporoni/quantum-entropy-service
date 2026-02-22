package com.leporonitech.quantum_keymanager.repository;

import com.leporonitech.quantum_keymanager.model.QuantumData;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para acesso aos registros de entropia quântica (tabela quantum_data).
 *
 * Fornece métodos para buscar registros não utilizados e contar a entropia disponível.
 * Estende JpaRepository que já fornece CRUD básico (save, findAll, deleteById, etc).
 *
 * Anotação:
 * - @Repository: Marca como componente de acesso a dados do Spring.
 */
@Repository
public interface QuantumDataRepository extends JpaRepository<QuantumData, Long> {

    /**
     * Busca todos os registros de entropia não utilizados, ordenados por ID (mais antigos primeiro).
     * Usa JPQL (Java Persistence Query Language).
     *
     * @return lista de QuantumData com used=false
     */
    @Query("SELECT q FROM QuantumData q WHERE q.used = false ORDER BY q.id ASC")
    List<QuantumData> findUnusedDataWithLock();
    
    /**
     * Busca uma quantidade limitada de registros não utilizados com lock pessimista (FOR UPDATE).
     *
     * O "FOR UPDATE" garante que, em cenários concorrentes, dois processos não consumam
     * o mesmo registro de entropia simultaneamente (exclusão mútua no banco).
     *
     * Usa SQL nativo porque o LIMIT com FOR UPDATE não é suportado diretamente em JPQL.
     *
     * @param limit quantidade máxima de registros a retornar
     * @return lista de QuantumData não utilizados (até o limite especificado)
     */
    @Query(value = "SELECT * FROM quantum_data WHERE used = false ORDER BY id ASC LIMIT :limit FOR UPDATE", nativeQuery = true)
    List<QuantumData> findUnusedDataWithLock(int limit);

    /**
     * Conta a quantidade de registros de entropia ainda não utilizados.
     * Usado pelo EntropyController para informar o status ao frontend
     * e pelo EntropyCollectorScheduler para decidir quando reabastecer.
     *
     * @return quantidade de registros com used=false
     */
    long countByUsedFalse();
}
