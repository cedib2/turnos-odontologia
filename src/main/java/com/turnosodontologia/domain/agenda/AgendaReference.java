package com.turnosodontologia.domain.agenda;

public record AgendaReference(String id) {
    public AgendaReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
