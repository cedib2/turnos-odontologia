package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.waitlist.EntradaWaitlist;

public record EntradaResponse(Long id, Long candidatoId, Long profesionalId, Long sillonId,
                              String inicioDeseado, String finDeseado, boolean depositoRequerido,
                              Long seniaId, String estado, Long turnoId) {

    public static EntradaResponse de(EntradaWaitlist entrada) {
        return new EntradaResponse(entrada.getId(), entrada.getCandidatoId(),
                entrada.getProfesionalId(), entrada.getSillonId(),
                entrada.getInicioDeseado() == null ? null : entrada.getInicioDeseado().toString(),
                entrada.getFinDeseado() == null ? null : entrada.getFinDeseado().toString(),
                entrada.isDepositoRequerido(), entrada.getSeniaId(), entrada.getEstado(),
                entrada.getTurnoId());
    }
}
