package com.turnosodontologia.historiapaciente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.clinico.HistoriaClinicaService;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import com.turnosodontologia.support.ActorConmutable;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({AccesoHistoriaConfig.class, ActorConmutable.Config.class, AccesoHistoriaPacientePostgresTest.Reloj.class})
@TestPropertySource(properties = {"PUBLIC_TOKEN_SECRET=secreto-publico-de-prueba-c13", "historia.copia.plazo-horas=48"})
class AccesoHistoriaPacientePostgresTest extends PostgresFlywayTest {

    private static final String EMISOR = "turnos-odontologia-public";
    private static final String EVIDENCIA = "evidencia-del-titular";

    @Autowired
    private AccesoHistoriaPacienteService acceso;

    @Autowired
    private PublicTokenService tokens;

    @Autowired
    private SimulatedVerificadorIdentidad identidad;

    @Autowired
    private HistoriaClinicaService historia;

    @Autowired
    private InMemoryAuditEventRecorder auditoria;

    @Autowired
    private RelojControlado reloj;

    private long pacienteId;

    @BeforeEach
    void preparar() {
        reloj.desplazamientoPorLectura(Duration.ZERO);
        identidad.limpiar();
        pacienteId = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, nombre, "
                + "apellido, telefono) VALUES ('DNI', '26111000', 'Rita', 'Mon', '+5491133334444') RETURNING id", Long.class);
        Long profesional = jdbcTemplate.queryForObject("INSERT INTO profesionales (matricula, nombre, apellido) "
                + "VALUES ('MP-C13', 'Dr', 'X') RETURNING id", Long.class);
        long ficha = historia.abrirFicha(pacienteId, null);
        historia.registrarOdontograma(ficha, 54, null, "caries");
        historia.registrarEvolucion(ficha, profesional, null, "evolución del titular", null);
        identidad.registrar(pacienteId, EVIDENCIA);
        auditoria.clear();
    }

    @Test
    void titularVerificadoConsultaSuHistoriaYElAccesoQuedaAuditado() {
        AccesoHistoriaPacienteService.Consulta consulta =
                acceso.consultar(token(PublicTokenScope.HISTORIA, pacienteId), EVIDENCIA);

        assertTrue(consulta.permitido());
        assertEquals(1, consulta.historia().odontograma().size());
        assertEquals("evolución del titular", consulta.historia().evoluciones().get(0).texto());
        AuditEvent evento = unico("historia.paciente.consultada");
        assertEquals(pacienteId, evento.entidadId());
        assertEquals("PACIENTE", evento.actor().modo());
        assertNotNull(evento.metadata().get("jti"));
    }

    @Test
    void identidadNoVerificadaDeniegaYRegistraElIntento() {
        String token = token(PublicTokenScope.HISTORIA, pacienteId);

        assertFalse(acceso.consultar(token, "otra-evidencia").permitido());
        assertFalse(acceso.consultar(token, null).permitido());
        AccesoHistoriaPacienteService.Copia copia = acceso.exportar(token, "");

        assertFalse(copia.permitido());
        assertNull(copia.documento());
        assertEquals("identidad_no_verificada", copia.motivo());
        List<AuditEvent> denegados = eventos("historia.paciente.denegada");
        assertEquals(3, denegados.size());
        assertTrue(denegados.stream().allMatch(e -> "identidad_no_verificada".equals(e.motivo())));
        assertEquals(List.of("consulta", "consulta", "exportacion"),
                denegados.stream().map(e -> e.metadata().get("operacion")).toList());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historia_copias", Integer.class));
    }

    @Test
    void tokenInvalidoDeOtroScopeVencidoOAdulteradoSeDeniegaYAudita() {
        assertEquals("scope_no_permitido",
                acceso.consultar(token(PublicTokenScope.RESERVAR, null), EVIDENCIA).motivo());
        assertEquals("token_no_valido", acceso.consultar("a.b.c", EVIDENCIA).motivo());
        assertEquals("token_no_valido", acceso.consultar(null, EVIDENCIA).motivo());
        String vencido = tokens.emitir(new PublicTokenClaims("jti-" + UUID.randomUUID(), EMISOR,
                String.valueOf(pacienteId), String.valueOf(pacienteId), PublicTokenScope.HISTORIA,
                Instant.now().minusSeconds(200_000).getEpochSecond(), 0, null, false));
        assertEquals("token_vencido", acceso.consultar(vencido, EVIDENCIA).motivo());
        assertEquals("token_no_valido", acceso.consultar(token(PublicTokenScope.HISTORIA, 999_999L), EVIDENCIA).motivo());

        assertEquals(5, eventos("historia.paciente.denegada").size());
        assertTrue(eventos("historia.paciente.consultada").isEmpty());
    }

    @Test
    void elTitularSoloAccedeASuPropiaHistoria() {
        Long otro = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, nombre, "
                + "apellido, telefono) VALUES ('DNI', '26111999', 'Otro', 'P', '+549') RETURNING id", Long.class);
        identidad.registrar(otro, "evidencia-del-otro");

        AccesoHistoriaPacienteService.Consulta propia = acceso.consultar(token(PublicTokenScope.HISTORIA, otro),
                "evidencia-del-otro");
        assertTrue(propia.permitido());
        assertNull(propia.historia(), "el otro paciente no tiene ficha: no ve la ajena");

        assertFalse(acceso.consultar(token(PublicTokenScope.HISTORIA, otro), EVIDENCIA).permitido());
    }

    @Test
    void copiaDentroDelPlazoConHashYRegistroAppendOnly() throws Exception {
        AccesoHistoriaPacienteService.Copia copia = acceso.exportar(token(PublicTokenScope.HISTORIA, pacienteId), EVIDENCIA);

        assertTrue(copia.permitido());
        assertTrue(copia.dentroDelPlazo());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(copia.documento())),
                copia.hashDocumento());
        JsonNode documento = new ObjectMapper().readTree(new String(copia.documento(), StandardCharsets.UTF_8));
        assertEquals(pacienteId, documento.path("pacienteId").asLong());
        assertEquals(54, documento.path("registros").path("odontograma").get(0).path("piezaFdi").asInt());
        Map<String, Object> fila = jdbcTemplate.queryForMap("SELECT * FROM historia_copias");
        assertEquals(copia.hashDocumento(), fila.get("hash_documento"));
        assertEquals(Duration.ofHours(48).toSeconds(), Duration.between(
                ((java.sql.Timestamp) fila.get("solicitada_at")).toInstant(),
                ((java.sql.Timestamp) fila.get("vence_at")).toInstant()).toSeconds());
        AuditEvent evento = unico("historia.paciente.exportada");
        assertEquals(true, evento.metadata().get("dentroDelPlazo"));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("DELETE FROM historia_copias"));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("UPDATE historia_copias SET token_jti = 'x'"));
    }

    @Test
    void entregaFueraDelPlazoQuedaRegistradaComoTal() {
        reloj.desplazamientoPorLectura(Duration.ofHours(49));

        AccesoHistoriaPacienteService.Copia copia = acceso.exportar(token(PublicTokenScope.HISTORIA, pacienteId), EVIDENCIA);

        assertTrue(copia.permitido());
        assertFalse(copia.dentroDelPlazo());
        assertEquals(false, unico("historia.paciente.exportada").metadata().get("dentroDelPlazo"));
    }

    @Test
    void validarDeC06SigueDenegandoHistoriaSinCambios() {
        PublicTokenService.Validacion validacion = tokens.validar(token(PublicTokenScope.HISTORIA, pacienteId));
        assertFalse(validacion.permitido());
        assertEquals("identidad_no_verificada", validacion.motivo());
    }

    @Test
    void fueraDeDevTestNingunaIdentidadSeVerifica() {
        assertFalse(new VerificadorIdentidadNoConfigurado().verificar(pacienteId, EVIDENCIA));
    }

    private String token(PublicTokenScope scope, Long paciente) {
        long ahora = Instant.now().getEpochSecond();
        String id = paciente == null ? null : String.valueOf(paciente);
        return tokens.emitir(new PublicTokenClaims("jti-" + UUID.randomUUID(), EMISOR, id, id, scope, ahora, 0,
                null, false));
    }

    private List<AuditEvent> eventos(String tipo) {
        return auditoria.eventos().stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    private AuditEvent unico(String tipo) {
        List<AuditEvent> lista = eventos(tipo);
        assertEquals(1, lista.size(), tipo);
        return lista.get(0);
    }

    /** Reloj que avanza un desplazamiento fijo en cada lectura, para simular entregas demoradas. */
    static class RelojControlado extends Clock {

        private volatile Duration paso = Duration.ZERO;
        private Instant actual = Instant.now();

        void desplazamientoPorLectura(Duration valor) {
            this.paso = valor;
            this.actual = Instant.now();
        }

        @Override
        public synchronized Instant instant() {
            Instant valor = actual;
            actual = actual.plus(paso);
            return valor;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    static class Reloj {

        @Bean
        @Primary
        RelojControlado relojControlado() {
            return new RelojControlado();
        }
    }
}
