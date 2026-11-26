package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Política RBAC de la superficie de cumplimiento y métricas de la Parte 4 (C-17, C-18, C-22).
 *
 * <p>Reutiliza la matriz {@code rol_permiso} de V001, sin migración nueva:
 * <ul>
 *   <li>Exportar, descargar una exportación, respaldar, restaurar y escribir en el dossier exigen
 *       {@code exportacion:crear}, que en el seed solo tiene el administrador (responsable de datos).</li>
 *   <li>Leer el dossier, las exportaciones vencidas y las métricas exige {@code auditoria:leer}
 *       (administrador y auditor).</li>
 * </ul>
 * El rol {@code paciente} nunca accede. {@code /precio} (C-21) es público y no pertenece a esta superficie.
 */
public class CumplimientoRbacPolicy implements RbacPolicy {

    static final String[] PATRONES = {"/cumplimiento", "/cumplimiento/**", "/metricas", "/metricas/**"};

    private static final List<String> PREFIJOS = List.of("/cumplimiento", "/metricas");
    private static final Set<String> METODOS_LECTURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final String ROL_PACIENTE = "paciente";

    private final RolPermisoRepository rolPermisos;

    public CumplimientoRbacPolicy(RolPermisoRepository rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

    @Override
    public String[] patrones() {
        return PATRONES.clone();
    }

    @Override
    public boolean cubre(String ruta) {
        return ruta != null && PREFIJOS.stream().anyMatch(p -> ruta.equals(p) || ruta.startsWith(p + "/"));
    }

    @Override
    public boolean permite(String codigoRol, String metodo, String ruta) {
        if (codigoRol == null || codigoRol.isBlank() || ROL_PACIENTE.equalsIgnoreCase(codigoRol)) {
            return false;
        }
        return requisito(metodo, ruta)
                .map(p -> rolPermisos.existsByRol_CodigoAndPermiso_RecursoAndPermiso_Accion(
                        codigoRol, p.recurso(), p.accion()))
                .orElse(false);
    }

    record Permiso(String recurso, String accion) {
    }

    static final Permiso EXPORTAR = new Permiso("exportacion", "crear");
    static final Permiso LEER_AUDITORIA = new Permiso("auditoria", "leer");

    static Optional<Permiso> requisito(String metodo, String ruta) {
        if (metodo == null || ruta == null) {
            return Optional.empty();
        }
        boolean lectura = METODOS_LECTURA.contains(metodo.toUpperCase(Locale.ROOT));
        if (ruta.equals("/metricas") || ruta.startsWith("/metricas/")) {
            return lectura ? Optional.of(LEER_AUDITORIA) : Optional.empty();
        }
        if (ruta.equals("/cumplimiento/dossier") || ruta.startsWith("/cumplimiento/dossier/")) {
            return Optional.of(lectura ? LEER_AUDITORIA : EXPORTAR);
        }
        if (lectura && ruta.equals("/cumplimiento/exportaciones/vencidas")) {
            return Optional.of(LEER_AUDITORIA);
        }
        if (ruta.startsWith("/cumplimiento/")) {
            return Optional.of(EXPORTAR);
        }
        return Optional.empty();
    }
}
