package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.DisponibilidadService;
import com.turnosodontologia.depositpayments.RefundResponse;
import com.turnosodontologia.depositpayments.SeniaResponse;
import com.turnosodontologia.depositpayments.SeniaService;
import com.turnosodontologia.depositpayments.SolicitudSenia;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Motor explícito de relleno (W-4). No hay listener ni consumo de
 * {@code auditoria_log} como bus: la entrada es el estado observable de un
 * turno cancelado cuya ocupación fue desactivada por C-09.
 */
@Service
@Transactional
public class RellenoWaitlistService {

    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final DisponibilidadService disponibilidad;
    private final ListaEsperaService listaEspera;
    private final WaitlistIdempotencyStore idempotencia;
    private final SeniaService senias;
    private final WaitlistProperties propiedades;
    private final CurrentActorProvider actores;

    public RellenoWaitlistService(TurnoRepository turnos, OcupacionRepository ocupaciones,
                                  DisponibilidadService disponibilidad, ListaEsperaService listaEspera,
                                  WaitlistIdempotencyStore idempotencia, SeniaService senias,
                                  WaitlistProperties propiedades, CurrentActorProvider actores) {
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.disponibilidad = disponibilidad;
        this.listaEspera = listaEspera;
        this.idempotencia = idempotencia;
        this.senias = senias;
        this.propiedades = propiedades;
        this.actores = actores;
    }

    /** Ofrece el hueco al primer candidato; no crea ocupación ni seña. */
    public ResultadoRelleno ofrecerTurnoLiberado(Long turnoId) {
        Turno turno = validarHueco(turnoId);
        if (idempotencia.esRepeticion(turnoId)) {
            throw new IllegalStateException("El hueco ya fue adjudicado");
        }
        EntradaWaitlist entrada = listaEspera.ofrecidaPorTurno(turnoId);
        if (entrada != null) {
            if (listaEspera.expirar(entrada) == null) {
                return new ResultadoRelleno(turnoId, entrada.getId(), "ofrecida_vigente", null);
            }
            entrada = null;
        }
        entrada = listaEspera.candidataPara(turno);
        if (entrada == null) {
            return new ResultadoRelleno(turnoId, null, "sin_candidato", null);
        }
        EntradaWaitlist ofrecida = listaEspera.ofrecer(entrada, turnoId);
        return new ResultadoRelleno(turnoId, ofrecida.getId(), ofrecida.getEstado(), "ofrecido");
    }

    /**
     * Adjudica el hueco en una sola transacción (W-7): idempotencia, ocupación,
     * reasignación, depósito opcional y evento. Cualquier fallo revierte todo.
     */
    public ResultadoRelleno adjudicarTurnoLiberado(Long turnoId) {
        Turno turno = validarHueco(turnoId);
        if (!idempotencia.registrar(turnoId)) {
            throw new IllegalStateException("Adjudicación duplicada para el turno " + turnoId);
        }
        EntradaWaitlist entrada = resolverCandidato(turno, turnoId);
        if (entrada == null) {
            throw new IllegalStateException("No hay candidato elegible para adjudicar el turno " + turnoId);
        }

        Long seniaId = null;
        if (entrada.isDepositoRequerido()) {
            String clave = "waitlist-senia-" + turnoId;
            SeniaResponse respuesta = senias.solicitar(
                    new SolicitudSenia(turnoId, null, propiedades.depositoMonto(), clave),
                    actores.current());
            seniaId = respuesta.id();
            listaEspera.vincularSenia(entrada, seniaId);
        }

        listaEspera.adjudicar(entrada, actores.current().nombre());
        ocupaciones.saveAndFlush(Ocupacion.paraTurno(turno.getId(),
                turno.getProfesional().getId(), turno.getSillon().getId(),
                turno.getInicio(), turno.getFin()));
        turno.reasignar(actores.current().nombre());
        listaEspera.publicarAdjudicacion(entrada, seniaId);
        return new ResultadoRelleno(turnoId, entrada.getId(), EntradaWaitlist.ADJUDICADA,
                seniaId == null ? "sin_deposito" : "con_deposito");
    }

    /** Reutiliza C-15 sin reabrirlo (W-8); el flag `conciliado` lo actualiza SeniaService. */
    public boolean conciliarDeposito(Long seniaId) {
        return senias.conciliar(seniaId);
    }

    /** Reembolso total y liberación inmediata del hueco (C-01 A4, W-8). */
    public RefundResponse reembolsarDeposito(Long seniaId, Long turnoId, ActorSimulado actor) {
        RefundResponse respuesta = senias.reembolsar(seniaId, actor);
        ocupaciones.findByTurnoIdAndActivoTrue(turnoId).ifPresent(Ocupacion::desactivar);
        return respuesta;
    }

    private EntradaWaitlist resolverCandidato(Turno turno, Long turnoId) {
        EntradaWaitlist entrada = listaEspera.ofrecidaPorTurno(turnoId);
        if (entrada != null && listaEspera.expirar(entrada) == null) {
            return entrada;
        }
        entrada = listaEspera.candidataPara(turno);
        return entrada == null ? null : listaEspera.ofrecer(entrada, turnoId);
    }

    private Turno validarHueco(Long turnoId) {
        if (turnoId == null) {
            throw new IllegalArgumentException("El turno liberado es obligatorio");
        }
        Turno turno = turnos.findById(turnoId)
                .orElseThrow(() -> new NoSuchElementException("Turno inexistente: " + turnoId));
        if (!"cancelado".equals(turno.getEstado())) {
            throw new IllegalStateException("El turno no está liberado");
        }
        if (ocupaciones.findByTurnoIdAndActivoTrue(turnoId).isPresent()) {
            throw new IllegalStateException("El hueco sigue ocupado");
        }
        if (!disponibilidad.huecoDisponible(turno.getProfesional().getId(), turno.getSillon().getId(),
                turno.getInicio(), turno.getFin())) {
            throw new IllegalStateException("El hueco volvió a estar ocupado por otra operación");
        }
        return turno;
    }
}
