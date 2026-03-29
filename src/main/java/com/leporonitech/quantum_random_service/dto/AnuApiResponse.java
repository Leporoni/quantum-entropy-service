package com.leporonitech.quantum_random_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnuApiResponse {
    private String type;
    private int length;
    private List<Integer> data;
    private boolean success;
}
