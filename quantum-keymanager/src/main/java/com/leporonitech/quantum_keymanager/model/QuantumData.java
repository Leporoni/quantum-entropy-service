package com.leporonitech.quantum_keymanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "quantum_data")
@Data
public class QuantumData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_base64", nullable = false, length = 2048)
    private String dataBase64;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "LFD";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Campo para marcar como usado (conforme plano de "Consume & Discard")
    @Column(name = "used", nullable = false)
    private boolean used = false;
}
