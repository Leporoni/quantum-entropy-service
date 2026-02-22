package com.leporonitech.quantum_keymanager.repository;

import com.leporonitech.quantum_keymanager.model.RsaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RsaKeyRepository extends JpaRepository<RsaKey, Long> {
    Optional<RsaKey> findByAlias(String alias);
}
