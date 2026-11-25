package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import com.turnosodontologia.infrastructure.persistence.EntradaWaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Alta, consulta, ofrecimiento simulado y expiración de la lista de espera. */
@Service
@Transactional
public class ListaEsperaService {

    private final EntradaWaitlistRepository entradas;
    private final CurrentActorProvider actores;
    private final NotificacionWaitlistPort notificaciones;
    private final WaitlistProperties propiedades;
    private final Clock reloj;
    private final WaitlistAuditPublisher auditoria;

    public ListaEsperaService(EntradaWaitlistRepository entradas, CurrentActorProvider actores,
                              NotificacionWaitlistPort notificaciones, WaitlistProperties propiedades,
                              Clock reloj, WaitlistAuditPublisher auditoria) {
        this.entradas = entradas;
        this.actores = actores;
        this.notificaciones = notificaciones;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.auditoria = auditoria;
    }

    public EntradaWaitlist registrar(AltaWaitlist alta) {
        if (alta == null) {
            throw new IllegalArgumentException("El alta es obligatoria");
        }
        boolean duplicada = entradas.findByCandidatoIdAndInicioDeseadoAndFinDeseadoAndActivoTrue(
                        alta.candidatoId(), alta.inicioDeseado(), alta.finDeseado())
                .isPresent();
        if (duplicada) {
            throw new IllegalArgumentException("Ya existe una entrada activa para ese candidato y rango");
        }
        ActorSimulado actor = actores.current();
        EntradaWaitlist entrada = new EntradaWaitlist(alta.candidatoId(), alta.profesionalId(),
                alta.sillonId(), alta.inicioDeseado(), alta.finDeseado(), alta.depositoRequerido());
        entrada.registrarCreacion(actor.nombre());
        EntradaWaitlist guardada = entradas.saveAndFlush(entrada);
        auditoria.publicar("waitlist.creado", "lista_espera", guardada.getId(), actor, "alta",
                WaitlistAuditMetadata.de(correlationId())
                        .entrada(guardada.getId()).sillon(guardada.getSillonId()));
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<EntradaWaitlist> porCandidato(Long candidatoId) {
        return entradas.findByCandidatoIdAndActivoTrueOrderByCreatedAtAsc(candidatoId);
    }

    @Transactional(readOnly = true)
    public EntradaWaitlist candidataPara(Turno turno) {
        if (turno == null || turno.getProfesional() == null || turno.getSillon() == null) {
            return null;
        }
        return entradas.findByEstadoAndActivoTrueOrderByCreatedAtAsc(EntradaWaitlist.ACTIVA)
                .stream()
                .filter(entrada -> entrada.cubre(turno.getProfesional().getId(), turno.getSillon().getId(),
                        turno.getInicio(), turno.getFin()))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public EntradaWaitlist ofrecidaPorTurno(Long turnoId) {
        return entradas.findByEstadoAndTurnoId(EntradaWaitlist.OFRECIDA, turnoId).orElse(null);
    }

    public EntradaWaitlist ofrecer(EntradaWaitlist entrada, Long turnoId) {
        ActorSimulado actor = actores.current();
        entrada.marcarOfrecida(turnoId, reloj.instant(), actor.nombre());
        EntradaWaitlist guardada = entradas.saveAndFlush(entrada);
        notificaciones.ofrecer(new OfertaWaitlist(guardada.getId(), turnoId));
        auditoria.publicar("waitlist.ofrecido", "lista_espera", guardada.getId(), actor, "oferta",
                WaitlistAuditMetadata.de(correlationId())
                        .entrada(guardada.getId()).turno(turnoId)
                        .sillon(guardada.getSillonId()));
        return guardada;
    }

    /** Devuelve la entrada expirada, o {@code null} cuando la oferta sigue vigente. */
    public EntradaWaitlist expirar(EntradaWaitlist entrada) {
        if (entrada == null || entrada.getOfrecidaEn() == null) {
            return null;
        }
        Instant vencimiento = entrada.getOfrecidaEn()
                .plusSeconds(propiedades.ofertaMinutos() * 60L);
        if (reloj.instant().isBefore(vencimiento)) {
            return null;
        }
        ActorSimulado actor = ActorSimulado.system();
        entrada.marcarExpirada(actor.nombre());
        EntradaWaitlist guardada = entradas.saveAndFlush(entrada);
        auditoria.publicar("waitlist.expirado", "lista_espera", guardada.getId(), actor, "oferta_vencida",
                WaitlistAuditMetadata.de(correlationId())
                        .entrada(guardada.getId()).turno(guardada.getTurnoId())
                        .sillon(guardada.getSillonId()));
        return guardada;
    }

    public EntradaWaitlist adjudicar(EntradaWaitlist entrada, String autor) {
        entrada.marcarAdjudicada(autor);
        return entradas.saveAndFlush(entrada);
    }

    public void publicarAdjudicacion(EntradaWaitlist entrada, Long seniaId) {
        auditoria.publicar("waitlist.adjudicado", "lista_espera", entrada.getId(),
                ActorSimulado.system(), "adjudicacion",
                WaitlistAuditMetadata.de(correlationId())
                        .entrada(entrada.getId()).turno(entrada.getTurnoId())
                        .senia(seniaId).sillon(entrada.getSillonId()));
    }

    public EntradaWaitlist vincularSenia(EntradaWaitlist entrada, Long seniaId) {
        entrada.vincularSenia(seniaId, actores.current().nombre());
        return entradas.saveAndFlush(entrada);
    }

    private String correlationId() {
        return UUID.randomUUID().toString();
    }
}
