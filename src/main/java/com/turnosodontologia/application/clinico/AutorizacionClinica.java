package com.turnosodontologia.application.clinico;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Permisos por rol sobre la historia clínica, según {@code rol_permiso} (recurso
 * {@code ficha_clinica}, seed V001). Solo actores de staff ({@code REAL} o {@code SIMULADO})
 * operan la historia por esta vía: {@code SYSTEM} no escribe historia y el paciente accede por
 * la superficie pública con token (C-13). Cada denegación se audita.
 */
@Component
public class AutorizacionClinica {

    static final String RECURSO = "ficha_clinica";

    private final RolPermisoRepository rolPermisos;
    private final AuditEventPublisher auditoria;

    public AutorizacionClinica(RolPermisoRepository rolPermisos, AuditEventPublisher auditoria) {
        this.rolPermisos = rolPermisos;
        this.auditoria = auditoria;
    }

    public void exigir(ActorSimulado actor, String accion, Long fichaId) {
        if (!permite(actor, accion)) {
            auditoria.publish(AuditEvent.de("clinico.acceso.denegado", "ficha", fichaId, actor,
                    "sin-permiso", Map.of("accion", accion), UUID.randomUUID().toString()));
            throw new AccesoClinicoDenegadoException("Sin permiso " + RECURSO + ":" + accion);
        }
    }

    boolean permite(ActorSimulado actor, String accion) {
        if (actor == null || actor.rol() == null
                || !("REAL".equals(actor.modo()) || "SIMULADO".equals(actor.modo()))) {
            return false;
        }
        String rol = actor.rol().toLowerCase(Locale.ROOT);
        return !"paciente".equals(rol)
                && rolPermisos.existsByRol_CodigoAndPermiso_RecursoAndPermiso_Accion(rol, RECURSO, accion);
    }
}
