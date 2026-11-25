package com.turnosodontologia.application.cumplimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Volcado y restauración genéricos del esquema de la aplicación (C-17). Trabaja sobre el catálogo de
 * PostgreSQL, así que las tablas que agreguen otras partes (C-10..C-16) quedan incluidas sin tocar este código.
 */
public final class VolcadoBaseDatos {

    static final String HISTORIAL_FLYWAY = "flyway_schema_history";

    private VolcadoBaseDatos() {
    }

    /** Qué incluir en un volcado. {@code filtros}: tabla → condición SQL con un único parámetro. */
    public record Seleccion(Set<String> tablasExcluidas, Map<String, Set<String>> columnasExcluidas,
                            Map<String, String> filtros, Object parametroFiltro, boolean soloFiltradas) {

        public static Seleccion completa() {
            return new Seleccion(Set.of(), Map.of(), Map.of(), null, false);
        }
    }

    /** Lee una instantánea consistente (REPEATABLE READ, solo lectura, UTC). */
    public static Paquete volcar(Connection conexion, String tipo, String alcance, String generadoAt,
                                 Seleccion seleccion, Map<String, Object> anexos) throws SQLException {
        boolean autoCommit = conexion.getAutoCommit();
        int aislamiento = conexion.getTransactionIsolation();
        conexion.setAutoCommit(false);
        conexion.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        try {
            ejecutar(conexion, "SET TRANSACTION READ ONLY");
            ejecutar(conexion, "SET LOCAL TimeZone = 'UTC'");
            Map<String, List<String>> tablas = new TreeMap<>();
            for (String tabla : tablas(conexion)) {
                if (seleccion.tablasExcluidas().contains(tabla)
                        || (seleccion.soloFiltradas() && !seleccion.filtros().containsKey(tabla))) {
                    continue;
                }
                tablas.put(tabla, filas(conexion, tabla,
                        seleccion.columnasExcluidas().getOrDefault(tabla, Set.of()),
                        seleccion.filtros().get(tabla), seleccion.parametroFiltro()));
            }
            Map<String, Long> secuencias = seleccion.soloFiltradas() ? Map.of() : secuencias(conexion);
            conexion.commit();
            return new Paquete(Paquete.VERSION, tipo, alcance, generadoAt, tablas, secuencias, anexos);
        } catch (SQLException | RuntimeException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setTransactionIsolation(aislamiento);
            conexion.setAutoCommit(autoCommit);
        }
    }

    /**
     * Restaura el paquete en una base ya migrada y vacía de datos de usuario, y verifica fila por fila
     * que el contenido restaurado coincide con el respaldo. Todo ocurre en una sola transacción.
     */
    public static Map<String, Integer> restaurarYVerificar(Connection destino, Paquete paquete)
            throws SQLException {
        boolean autoCommit = destino.getAutoCommit();
        destino.setAutoCommit(false);
        try {
            ejecutar(destino, "SET LOCAL TimeZone = 'UTC'");
            Set<String> existentes = new LinkedHashSet<>(tablas(destino));
            for (String tabla : paquete.tablas().keySet()) {
                if (!existentes.contains(tabla)) {
                    throw new IllegalStateException("La tabla " + tabla + " no existe en el entorno aislado");
                }
            }
            List<String> orden = ordenTopologico(destino, paquete.tablas().keySet());
            // Las tablas cuyo contenido ya coincide con el respaldo (seeds de migración, como el
            // catálogo FDI append-only) no se reescriben: borrarlas violaría sus triggers.
            Set<String> identicas = new LinkedHashSet<>();
            for (String tabla : orden) {
                List<String> esperadas = new ArrayList<>(paquete.tablas().get(tabla));
                List<String> actuales = filas(destino, tabla, Set.of(), null, null);
                esperadas.sort(null);
                actuales.sort(null);
                if (esperadas.equals(actuales)) {
                    identicas.add(tabla);
                }
            }
            for (int i = orden.size() - 1; i >= 0; i--) {
                if (!identicas.contains(orden.get(i))) {
                    ejecutar(destino, "DELETE FROM " + identificador(orden.get(i)));
                }
            }
            for (String tabla : orden) {
                if (!identicas.contains(tabla)) {
                    insertar(destino, tabla, paquete.tablas().get(tabla));
                }
            }
            for (Map.Entry<String, Long> secuencia : paquete.secuencias().entrySet()) {
                if (secuencia.getValue() != null) {
                    try (PreparedStatement ps = destino.prepareStatement("SELECT setval(?::regclass, ?, true)")) {
                        ps.setString(1, identificador(secuencia.getKey()));
                        ps.setLong(2, secuencia.getValue());
                        ps.execute();
                    }
                }
            }
            Map<String, Integer> verificadas = new LinkedHashMap<>();
            for (String tabla : orden) {
                List<String> esperadas = new ArrayList<>(paquete.tablas().get(tabla));
                List<String> restauradas = filas(destino, tabla, Set.of(), null, null);
                esperadas.sort(null);
                restauradas.sort(null);
                if (!esperadas.equals(restauradas)) {
                    throw new IllegalStateException("Verificación fallida en la tabla " + tabla
                            + ": esperadas " + esperadas.size() + " filas, restauradas " + restauradas.size());
                }
                verificadas.put(tabla, restauradas.size());
            }
            destino.commit();
            return verificadas;
        } catch (SQLException | RuntimeException e) {
            destino.rollback();
            throw e;
        } finally {
            destino.setAutoCommit(autoCommit);
        }
    }

    /** Identidad del servidor y la base, para impedir restaurar sobre la base productiva. */
    public static String identidad(Connection conexion) throws SQLException {
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_database() || '@' "
                     + "|| coalesce(host(inet_server_addr()), 'local') || ':' "
                     + "|| coalesce(inet_server_port()::text, '-')")) {
            rs.next();
            return rs.getString(1);
        }
    }

    static List<String> tablas(Connection conexion) throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() "
                        + "AND tablename <> ? ORDER BY tablename")) {
            ps.setString(1, HISTORIAL_FLYWAY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tablas.add(rs.getString(1));
                }
            }
        }
        return tablas;
    }

    private static List<String> filas(Connection conexion, String tabla, Set<String> columnasExcluidas,
                                      String filtro, Object parametro) throws SQLException {
        StringBuilder expresion = new StringBuilder("to_jsonb(t)");
        for (String columna : new TreeSet<>(columnasExcluidas)) {
            expresion.append(" - '").append(columna.replace("'", "''")).append('\'');
        }
        List<String> clavePrimaria = clavePrimaria(conexion, tabla);
        String orden = clavePrimaria.isEmpty()
                ? "1"
                : String.join(", ", clavePrimaria.stream().map(c -> "t." + identificador(c)).toList());
        String sql = "SELECT (" + expresion + ")::text FROM " + identificador(tabla) + " t"
                + (filtro == null ? "" : " WHERE " + filtro) + " ORDER BY " + orden;
        List<String> filas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (filtro != null) {
                ps.setObject(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(rs.getString(1));
                }
            }
        }
        return filas;
    }

    private static List<String> clavePrimaria(Connection conexion, String tabla) throws SQLException {
        List<String> columnas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement("""
                SELECT a.attname
                FROM pg_index i
                JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
                WHERE i.indrelid = ?::regclass AND i.indisprimary
                ORDER BY array_position(i.indkey::int2[], a.attnum)
                """)) {
            ps.setString(1, identificador(tabla));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columnas.add(rs.getString(1));
                }
            }
        }
        return columnas;
    }

    private static Map<String, Long> secuencias(Connection conexion) throws SQLException {
        Map<String, Long> secuencias = new TreeMap<>();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT sequencename, last_value FROM pg_sequences "
                     + "WHERE schemaname = current_schema()")) {
            while (rs.next()) {
                secuencias.put(rs.getString(1), (Long) rs.getObject(2));
            }
        }
        return secuencias;
    }

    private static void insertar(Connection destino, String tabla, List<String> filas) throws SQLException {
        if (filas.isEmpty()) {
            return;
        }
        List<String> columnas = new ArrayList<>();
        try (PreparedStatement ps = destino.prepareStatement("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND is_generated = 'NEVER'
                ORDER BY ordinal_position
                """)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columnas.add(identificador(rs.getString(1)));
                }
            }
        }
        String lista = String.join(", ", columnas);
        String sql = "INSERT INTO " + identificador(tabla) + " (" + lista + ") SELECT " + lista
                + " FROM jsonb_populate_record(NULL::" + identificador(tabla) + ", ?::jsonb)";
        try (PreparedStatement ps = destino.prepareStatement(sql)) {
            for (String fila : filas) {
                ps.setString(1, fila);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static List<String> ordenTopologico(Connection conexion, Set<String> tablas) throws SQLException {
        Map<String, Set<String>> padres = new HashMap<>();
        tablas.forEach(t -> padres.put(t, new TreeSet<>()));
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT hija.relname, padre.relname
                     FROM pg_constraint c
                     JOIN pg_class hija ON hija.oid = c.conrelid
                     JOIN pg_class padre ON padre.oid = c.confrelid
                     JOIN pg_namespace n ON n.oid = hija.relnamespace
                     WHERE c.contype = 'f' AND n.nspname = current_schema() AND hija.oid <> padre.oid
                     """)) {
            while (rs.next()) {
                String hija = rs.getString(1);
                String padre = rs.getString(2);
                if (padres.containsKey(hija) && padres.containsKey(padre)) {
                    padres.get(hija).add(padre);
                }
            }
        }
        List<String> orden = new ArrayList<>();
        Set<String> visitadas = new LinkedHashSet<>();
        for (String tabla : new TreeSet<>(tablas)) {
            visitar(tabla, padres, visitadas, new ArrayDeque<>(), orden);
        }
        return orden;
    }

    private static void visitar(String tabla, Map<String, Set<String>> padres, Set<String> visitadas,
                                Deque<String> camino, List<String> orden) {
        if (visitadas.contains(tabla)) {
            return;
        }
        if (camino.contains(tabla)) {
            throw new IllegalStateException("Ciclo de claves foráneas en " + camino);
        }
        camino.push(tabla);
        for (String padre : padres.get(tabla)) {
            visitar(padre, padres, visitadas, camino, orden);
        }
        camino.pop();
        visitadas.add(tabla);
        orden.add(tabla);
    }

    static String identificador(String nombre) {
        return '"' + nombre.replace("\"", "\"\"") + '"';
    }

    private static void ejecutar(Connection conexion, String sql) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.execute(sql);
        }
    }
}
