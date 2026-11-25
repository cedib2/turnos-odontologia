package com.turnosodontologia.invoicing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Implementación NO-OP/simulada: registra el aviso, nunca envía nada. */
public final class AvisoRecepcionSimulado implements AvisoRecepcionPort {

    private final List<Aviso> avisos = new CopyOnWriteArrayList<>();

    @Override
    public void solicitar(Long seniaId, String motivo) {
        avisos.add(new Aviso(seniaId, motivo));
    }

    public List<Aviso> avisos() {
        return List.copyOf(avisos);
    }

    /** Limpia el registro simulado entre pruebas sin exponer la lista interna. */
    public void reset() {
        avisos.clear();
    }

    public record Aviso(Long seniaId, String motivo) {
    }
}
