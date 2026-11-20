package com.turnosodontologia.domain.identidad;

public record IdentidadReference(String id) {
    public IdentidadReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
