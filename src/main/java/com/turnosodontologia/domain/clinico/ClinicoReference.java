package com.turnosodontologia.domain.clinico;

public record ClinicoReference(String id) {
    public ClinicoReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
