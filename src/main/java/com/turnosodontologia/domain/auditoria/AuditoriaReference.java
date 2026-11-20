package com.turnosodontologia.domain.auditoria;

public record AuditoriaReference(String id) {
    public AuditoriaReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
