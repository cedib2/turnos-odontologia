package com.turnosodontologia.audit;

import java.util.Set;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Set.of(
            "turno.creado",
            "turno.reprogramado",
            "turno.sobreturno",
            "bloqueo.creado",
            "publictoken.historia.denegado",
            // C-17 export-backup-restore
            "exportacion.solicitada",
            "exportacion.completada",
            "exportacion.fallida",
            "exportacion.descargada",
            "respaldo.completado",
            "respaldo.fallido",
            "restore.verificado",
            "restore.fallido",
            // C-18 compliance-evidence-dossier
            "dossier.evidencia_registrada",
            "dossier.subprocesador_registrado",
            "dossier.descargado",
            "dossier.publicado",
            "dossier.publicacion_bloqueada",
            // C-22 adoption-metrics-dashboard
            "metricas.consultadas");

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
