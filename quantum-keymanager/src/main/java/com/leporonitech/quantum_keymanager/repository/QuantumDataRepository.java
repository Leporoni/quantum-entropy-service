package com.leporonitech.quantum_keymanager.repository;

import com.leporonitech.quantum_keymanager.model.QuantumData;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuantumDataRepository extends JpaRepository<QuantumData, Long> {

    @Query("SELECT q FROM QuantumData q WHERE q.used = false ORDER BY q.id ASC")
    List<QuantumData> findUnusedDataWithLock();
    
    // Correção: Lock mode via SQL nativo (FOR UPDATE) em vez de anotação @Lock
    @Query(value = "SELECT * FROM quantum_data WHERE used = false AND source = :source ORDER BY id ASC LIMIT :limit FOR UPDATE", nativeQuery = true)
    List<QuantumData> findUnusedDataWithLock(String source, int limit);

    org.springframework.data.domain.Page<com.leporonitech.quantum_keymanager.model.QuantumData> findByUsedFalseAndSource(String source, org.springframework.data.domain.Pageable pageable);

    long countByUsedFalse();

    long countByUsedFalseAndSource(String source);
}
