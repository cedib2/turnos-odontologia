package com.turnosodontologia.application.metricas;

import java.util.List;

/**
 * Métricas del tablero de adopción con la metodología aprobada en C-01 §Métricas.
 * Cambiar una metodología requiere aprobación del PO. Las metas iniciales quedan
 * {@code (TBD)} hasta el piloto: no se inventan cifras.
 */
public final class CatalogoMetricas {

    public static final String ADOPCION_ONLINE = "adopcion_online";
    public static final String CUMPLIMIENTO_VENTANA = "cumplimiento_ventana";
    public static final String CONFIRMACIONES = "confirmaciones";
    public static final String EVIDENCIA_CUMPLIMIENTO = "evidencia_cumplimiento";

    static final String META_TBD = "(TBD) — baseline del piloto y umbral revisable";

    public record Definicion(String codigo, MetodologiaMetrica metodologia, String dependencia) {
    }

    public static final List<Definicion> DEFINICIONES = List.of(
            new Definicion(ADOPCION_ONLINE, new MetodologiaMetrica(
                    "% de turnos creados por el paciente vía link/QR sobre el total de turnos creados en el período",
                    "100 × turnos con token público de creación (link/QR) / total de turnos creados",
                    "turnos.created_at y turnos.token_publico (canal de creación por token público, C-01 §Métricas)",
                    META_TBD), "C-03/C-08 (disponible)"),
            new Definicion(CUMPLIMIENTO_VENTANA, new MetodologiaMetrica(
                    "% de cancelaciones/reprogramaciones realizadas dentro de la ventana de 48 hs hábiles",
                    "100 × cancelaciones/reprogramaciones dentro de 48 hs hábiles / total de cancelaciones/reprogramaciones",
                    "Eventos de cancelación/reprogramación con sello de tiempo y calendario de feriados",
                    META_TBD), "C-09 public-booking-and-reschedule (cómputo de 48 hs hábiles)"),
            new Definicion(CONFIRMACIONES, new MetodologiaMetrica(
                    "% de turnos confirmados por el paciente sobre los turnos con recordatorio enviado",
                    "100 × turnos confirmados / turnos con recordatorio enviado",
                    "Eventos de confirmación de la outbox de WhatsApp",
                    META_TBD), "C-10 whatsapp-notifications-outbox"),
            new Definicion(EVIDENCIA_CUMPLIMIENTO, new MetodologiaMetrica(
                    "Dossier descargable completo (DPA, AAIP, RNBD, logs, exportación)",
                    "100 × ítems del dossier presentes y verificables / ítems exigidos",
                    "Dossier de cumplimiento, logs append-only, exportación y respaldo",
                    META_TBD), "C-18 compliance-evidence-dossier"));

    private CatalogoMetricas() {
    }
}
