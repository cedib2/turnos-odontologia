package com.turnosodontologia.domain.financiero;

public record FinancieroReference(String id) {
    public FinancieroReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
