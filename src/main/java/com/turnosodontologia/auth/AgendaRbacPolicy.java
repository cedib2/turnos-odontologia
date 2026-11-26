package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Política RBAC de la superficie de agenda, catálogos de scheduling, directorio (pacientes, profesionales y
 * sillones) y ficha clínica.
 *
 * <p>Se apoya exclusivamente en la matriz {@code rol_permiso} sembrada en V001 (sin migración
 * nueva). Cada operación exige que el rol tenga <em>todos</em> los permisos de al menos una
 * de sus alternativas. El rol {@code paciente} nunca accede a la superficie de staff: el
 * paciente opera solo por las rutas públicas con token (C-06/C-09/C-13).
 */
public class AgendaRbacPolicy implements RbacPolicy {

    static final List<String> PREFIJOS_AGENDA = List.of("/agenda", "/turnos", "/bloqueos", "/sobreturnos");
    static final List<String> PREFIJOS_CATALOGO = List.of("/tratamientos", "/horarios", "/feriados");
    /** Directorio del frontend: pacientes (alta y link de reserva), profesionales (vínculo) y sillones. */
    static final List<String> PREFIJOS_DIRECTORIO = List.of("/pacientes", "/profesionales", "/sillones");
    static final List<String> PREFIJOS_FICHA = List.of("/fichas");
    static final String[] PATRONES = {
            "/agenda", "/agenda/**", "/turnos", "/turnos/**", "/bloqueos", "/bloqueos/**",
            "/sobreturnos", "/sobreturnos/**", "/tratamientos", "/tratamientos/**",
            "/horarios", "/horarios/**", "/feriados", "/feriados/**",
            "/pacientes", "/pacientes/**", "/profesionales", "/profesionales/**", "/sillones", "/sillones/**",
            "/fichas", "/fichas/**"};

    private static final Set<String> METODOS_LECTURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final String ROL_PACIENTE = "paciente";

    record Permiso(String recurso, String accion) {
    }

    private static final List<List<Permiso>> LEER_AGENDA = List.of(
            List.of(new Permiso("agenda", "leer")),
            List.of(new Permiso("turno", "leer")));
    private static final List<List<Permiso>> CREAR_AGENDA = List.of(
            List.of(new Permiso("agenda", "crear")));
    private static final List<List<Permiso>> ACTUALIZAR_AGENDA = List.of(
            List.of(new Permiso("agenda", "actualizar")));
    private static final List<List<Permiso>> LEER_CATALOGO = List.of(
            List.of(new Permiso("configuracion", "leer")),
            List.of(new Permiso("agenda", "leer")),
            List.of(new Permiso("agenda", "crear")),
            List.of(new Permiso("turno", "leer")));
    private static final List<List<Permiso>> ESCRIBIR_CATALOGO = List.of(
            List.of(new Permiso("configuracion", "leer"), new Permiso("agenda", "actualizar")));

    private static final List<List<Permiso>> LEER_FICHA = List.of(
            List.of(new Permiso("ficha_clinica", "leer")));
    private static final List<List<Permiso>> ESCRIBIR_FICHA = List.of(
            List.of(new Permiso("ficha_clinica", "crear")));

    private final RolPermisoRepository rolPermisos;

    public AgendaRbacPolicy(RolPermisoRepository rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

    @Override
    public String[] patrones() {
        return PATRONES.clone();
    }

    @Override
    public boolean cubre(String ruta) {
        return aplicaA(ruta);
    }

    public static boolean aplicaA(String ruta) {
        return prefijo(ruta, PREFIJOS_AGENDA).isPresent() || prefijo(ruta, PREFIJOS_CATALOGO).isPresent()
                || prefijo(ruta, PREFIJOS_DIRECTORIO).isPresent() || prefijo(ruta, PREFIJOS_FICHA).isPresent();
    }

    @Override
    public boolean permite(String codigoRol, String metodo, String ruta) {
        if (codigoRol == null || codigoRol.isBlank() || ROL_PACIENTE.equalsIgnoreCase(codigoRol)) {
            return false;
        }
        Optional<List<List<Permiso>>> requisito = requisito(metodo, ruta);
        if (requisito.isEmpty()) {
            return false;
        }
        return requisito.get().stream().anyMatch(alternativa -> alternativa.stream()
                .allMatch(permiso -> rolPermisos.existsByRol_CodigoAndPermiso_RecursoAndPermiso_Accion(
                        codigoRol, permiso.recurso(), permiso.accion())));
    }

    static Optional<List<List<Permiso>>> requisito(String metodo, String ruta) {
        if (metodo == null || ruta == null) {
            return Optional.empty();
        }
        boolean lectura = METODOS_LECTURA.contains(metodo.toUpperCase(Locale.ROOT));
        Optional<String> agenda = prefijo(ruta, PREFIJOS_AGENDA);
        if (agenda.isPresent()) {
            if (lectura) {
                return Optional.of(LEER_AGENDA);
            }
            return switch (agenda.get()) {
                // Crear es el POST a la colección; cancelar o reprogramar (POST /turnos/{id}/...) es actualizar.
                case "/turnos", "/bloqueos" -> Optional.of(
                        "POST".equalsIgnoreCase(metodo) && ruta.equals(agenda.get()) ? CREAR_AGENDA : ACTUALIZAR_AGENDA);
                case "/sobreturnos" -> Optional.of(ACTUALIZAR_AGENDA);
                default -> Optional.of(ACTUALIZAR_AGENDA);
            };
        }
        if (prefijo(ruta, PREFIJOS_CATALOGO).isPresent()) {
            return Optional.of(lectura ? LEER_CATALOGO : ESCRIBIR_CATALOGO);
        }
        Optional<String> directorio = prefijo(ruta, PREFIJOS_DIRECTORIO);
        if (directorio.isPresent()) {
            if (lectura) {
                return Optional.of(LEER_AGENDA);
            }
            // Alta de paciente y link de reserva los hace recepción; el vínculo con un profesional, administración.
            return Optional.of("/pacientes".equals(directorio.get()) && "POST".equalsIgnoreCase(metodo)
                    ? CREAR_AGENDA : ESCRIBIR_CATALOGO);
        }
        if (prefijo(ruta, PREFIJOS_FICHA).isPresent()) {
            return Optional.of(lectura ? LEER_FICHA : ESCRIBIR_FICHA);
        }
        return Optional.empty();
    }

    private static Optional<String> prefijo(String ruta, List<String> prefijos) {
        if (ruta == null) {
            return Optional.empty();
        }
        return prefijos.stream()
                .filter(p -> ruta.equals(p) || ruta.startsWith(p + "/"))
                .findFirst();
    }
}
