package com.turnosodontologia.cumplimiento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.cumplimiento.ExportacionService;
import com.turnosodontologia.application.cumplimiento.OperacionCumplimiento;
import com.turnosodontologia.application.cumplimiento.RespaldoService;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.support.PostgresFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C-17: exportación completa, copia por paciente, respaldo cifrado y restore en entorno aislado. */
class ExportacionRespaldoRestoreTest extends CumplimientoPostgresTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ExportacionService exportaciones;

    @Autowired
    private RespaldoService respaldos;

    @Autowired
    private CumplimientoFixtures fixtures;

    private Paciente ana;
    private Paciente beto;

    @BeforeEach
    void sembrar() {
        fixtures.sembrarCatalogo();
        ana = fixtures.paciente("DOC-ANA", "Ana");
        beto = fixtures.paciente("DOC-BETO", "Beto");
        fixtures.turno(ana, "S-P4-1", 9, "recepcion");
        fixtures.turno(beto, "S-P4-2", 10, "recepcion");
        jdbcTemplate.update("INSERT INTO usuarios (rol_id, email, hash_password) "
                + "VALUES (4, 'admin.p4@consultorio.test', 'HASH-SECRETO-P4')");
    }

    @Test
    void exportacionCompletaCifradaSinCredencialesYAuditada() throws Exception {
        OperacionCumplimiento operacion = exportaciones.exportar("COMPLETA", null, "pedido del responsable");

        assertEquals("COMPLETADA", operacion.estado());
        assertTrue(operacion.cifrado());
        assertEquals(64, operacion.checksumSha256().length());
        assertNotNull(operacion.plazoVenceAt());
        assertEquals(Duration.ofHours(48), Duration.between(operacion.solicitadoAt(), operacion.plazoVenceAt()));

        byte[] enDisco = Files.readAllBytes(artefacto(operacion));
        assertFalse(new String(enDisco, StandardCharsets.ISO_8859_1).contains("DOC-ANA"),
                "el artefacto en reposo no puede contener datos personales en claro");

        JsonNode contenido = mapper.readTree(exportaciones.descargar(operacion.id(), "verificación"));
        JsonNode tablas = contenido.get("tablas");
        assertEquals(2, tablas.get("pacientes").size());
        assertEquals(2, tablas.get("turnos").size());
        assertTrue(tablas.has("auditoria_log"));
        assertFalse(tablas.has("auth_sesiones"));
        assertFalse(tablas.has("auth_tokens_refresh"));
        assertFalse(tablas.has("public_token_jti"));
        assertFalse(contenido.toString().contains("HASH-SECRETO-P4"));
        assertTrue(tablas.get("usuarios").get(0).asText().contains("admin.p4@consultorio.test"));

        assertEquals(List.of("exportacion.solicitada", "exportacion.completada", "exportacion.descargada"),
                acciones("cumplimiento_operacion"));
    }

    @Test
    void copiaPorPacienteSoloIncluyeSusDatosYDeclaraHistoriaPendiente() throws Exception {
        OperacionCumplimiento operacion = exportaciones.exportar("PACIENTE", ana.getId(), "Ley 26.529 art. 14");

        JsonNode contenido = mapper.readTree(exportaciones.descargar(operacion.id(), null));
        JsonNode tablas = contenido.get("tablas");
        assertEquals(1, tablas.get("pacientes").size());
        assertTrue(tablas.get("pacientes").get(0).asText().contains("DOC-ANA"));
        assertEquals(1, tablas.get("turnos").size());
        assertFalse(contenido.toString().contains("DOC-BETO"));
        assertEquals(2, tablas.size());
        assertTrue(contenido.get("anexos").get("historiaClinica").asText().contains("C-13"));
        assertEquals(ana.getId(), jdbcTemplate.queryForObject(
                "SELECT (metadata->>'pacienteId')::bigint FROM auditoria_log WHERE accion = 'exportacion.completada'",
                Long.class));
    }

    @Test
    void solicitudesSinCompletarFueraDePlazoSeListanComoVencidas() {
        OperacionCumplimiento pendiente = exportaciones.solicitar("PACIENTE", beto.getId(), "copia");

        assertTrue(exportaciones.vencidas(Instant.now()).isEmpty());
        List<OperacionCumplimiento> vencidas = exportaciones.vencidas(Instant.now().plus(Duration.ofHours(49)));
        assertEquals(List.of(pendiente.id()), vencidas.stream().map(OperacionCumplimiento::id).toList());
    }

    @Test
    void exportacionInvalidaSeRechazaSinRegistrar() {
        assertThrows(IllegalArgumentException.class, () -> exportaciones.solicitar("PACIENTE", null, "x"));
        assertThrows(IllegalArgumentException.class, () -> exportaciones.solicitar("PARCIAL", null, "x"));
        assertEquals(0, contar("SELECT count(*) FROM cumplimiento_operaciones"));
    }

    @Test
    void respaldoYRestoreVerificadoEnEntornoAislado() throws Exception {
        OperacionCumplimiento respaldo = respaldos.respaldar("respaldo manual");
        assertEquals("COMPLETADA", respaldo.estado());
        assertTrue(respaldo.cifrado());
        assertEquals(respaldo.completadoAt().atZone(ZoneId.of("America/Argentina/Buenos_Aires")).plusYears(10)
                .toInstant(), respaldo.retenerHasta());

        long pacientesProductivos = contar("SELECT count(*) FROM pacientes");
        OperacionCumplimiento restore = respaldos.restaurarEnAislado(respaldo.id(), "prueba de restore");

        assertEquals("COMPLETADA", restore.estado(), restore.error());
        assertEquals(respaldo.id(), restore.respaldoOrigenId());
        assertEquals(respaldo.checksumSha256(), restore.checksumSha256());
        assertEquals(respaldo.filas(), restore.filas());
        assertTrue(restore.entornoRestore().endsWith("/" + CumplimientoEntorno.BASE_AISLADA));

        try (Connection aislada = PostgresFixture.open(CumplimientoEntorno.BASE_AISLADA);
             Statement st = aislada.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*), "
                     + "(SELECT hash_password FROM usuarios WHERE email = 'admin.p4@consultorio.test') FROM pacientes")) {
            rs.next();
            assertEquals(pacientesProductivos, rs.getLong(1));
            assertEquals("HASH-SECRETO-P4", rs.getString(2), "el respaldo sí incluye credenciales (cifradas)");
        }
        assertEquals(pacientesProductivos, contar("SELECT count(*) FROM pacientes"));
        assertEquals(List.of("respaldo.completado", "restore.verificado"), acciones("cumplimiento_operacion"));
    }

    @Test
    void respaldoAlteradoEnDestinoNoSeRestaura() throws Exception {
        OperacionCumplimiento respaldo = respaldos.respaldar("respaldo manual");
        Path archivo = artefacto(respaldo);
        byte[] contenido = Files.readAllBytes(archivo);
        contenido[contenido.length - 1] ^= 0x01;
        Files.write(archivo, contenido);

        OperacionCumplimiento restore = respaldos.restaurarEnAislado(respaldo.id(), "prueba");

        assertEquals("FALLIDA", restore.estado());
        assertTrue(restore.error().contains("alterado"), restore.error());
        assertEquals(List.of("respaldo.completado", "restore.fallido"), acciones("cumplimiento_operacion"));
    }

    @Test
    void registroDeOperacionesEsInmutableUnaVezCerrado() {
        OperacionCumplimiento respaldo = respaldos.respaldar("respaldo manual");

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE cumplimiento_operaciones SET checksum_sha256 = repeat('0', 64) WHERE id = ?", respaldo.id()));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "DELETE FROM cumplimiento_operaciones WHERE id = ?", respaldo.id()));
    }

    private Path artefacto(OperacionCumplimiento operacion) {
        return CumplimientoEntorno.DIRECTORIO.resolve(operacion.destino().substring("dir:".length()));
    }

    private List<String> acciones(String entidad) {
        return jdbcTemplate.queryForList("SELECT accion FROM auditoria_log WHERE entidad = ? ORDER BY id",
                String.class, entidad);
    }

    private long contar(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
