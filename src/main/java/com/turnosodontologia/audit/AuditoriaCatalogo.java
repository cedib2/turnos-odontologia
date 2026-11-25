package com.turnosodontologia.audit;

import com.turnosodontologia.export.ExportAuditEvents;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Stream.concat(
            Stream.of(
                    "turno.creado",
                    "turno.confirmado",
                    "turno.cancelado",
                    "turno.reprogramado",
                    "turno.sobreturno",
                    "bloqueo.creado",
                    "publictoken.historia.denegado",
                    "notificacion.programada",
                    "notificacion.enviada",
                    "notificacion.bloqueada",
                    "notificacion.fallida",
                    "notificacion.confirmada",
                    "whatsapp.webhook.rechazado",
                    "clinico.ficha.abierta",
                    "clinico.anamnesis.registrada",
                    "clinico.odontograma.registrado",
                    "clinico.evolucion.registrada",
                    "clinico.consentimiento.registrado",
                    "clinico.acceso.denegado",
                    "clinico.evolucion.firmada",
                    "clinico.evolucion.firma_rechazada",
                    "clinico.consentimiento.firmado",
                    "clinico.consentimiento.firma_fallida",
                    "historia.paciente.consultada",
                    "historia.paciente.exportada",
                    "historia.paciente.denegada",
                    "presupuesto.creado",
                    "presupuesto.modificado",
                    "pago.solicitado",
                    "pago.aprobado",
                    "pago.rechazado",
                    "pago.reembolsado",
                    "pago.conciliado",
                    "factura.emitida",
                    "factura.rechazada",
                    "factura.conciliada",
                    "factura.anulada",
                    "nota_credito.emitida",
                    // C-19 (W-10): catálogo ampliado por acta PO del 2026-11-23.
                    "waitlist.creado",
                    "waitlist.ofrecido",
                    "waitlist.adjudicado",
                    "waitlist.expirado",
                    "turno.inasistencia",
                    "metrica.publicada",
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
                    "metricas.consultadas"),
            ExportAuditEvents.TIPOS.stream())
            .collect(Collectors.toUnmodifiableSet());

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
