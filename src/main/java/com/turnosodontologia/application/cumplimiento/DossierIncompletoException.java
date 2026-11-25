package com.turnosodontologia.application.cumplimiento;

import java.util.List;

/** La publicación del dossier se bloquea mientras falte evidencia obligatoria (C-18). */
public class DossierIncompletoException extends RuntimeException {

    private final List<String> pendientes;

    public DossierIncompletoException(List<String> pendientes) {
        super("Publicación bloqueada: falta evidencia obligatoria " + pendientes);
        this.pendientes = List.copyOf(pendientes);
    }

    public List<String> pendientes() {
        return pendientes;
    }
}
