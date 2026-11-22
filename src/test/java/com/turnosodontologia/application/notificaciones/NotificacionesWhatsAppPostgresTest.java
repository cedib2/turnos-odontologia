package com.turnosodontologia.application.notificaciones;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = {
        "WHATSAPP_APP_SECRET=secreto-webhook-de-prueba",
        "notificaciones.max-intentos=2",
        "notificaciones.reintento-segundos=60"})
class NotificacionesWhatsAppPostgresTest extends PostgresFlywayTest {

    private static final Instant AHORA = Instant.parse("2026-12-10T12:00:00Z");

    @Autowired
    private NotificacionService notificaciones;

    @Autowired
    private OutboxDispatcher dispatcher;

    @Autowired
    private WhatsAppWebhookService webhook;

    @Autowired
    private WebhookSignatureVerifier verificador;

    @Autowired
    private NotificacionStore store;

    @Autowired
    private SimulatedWhatsAppGateway gateway;

    @Autowired
    private InMemoryAuditEventRecorder auditoria;

    private long turnoId;

    @BeforeEach
    void preparar() {
        gateway.limpiar();
        auditoria.clear();
        turnoId = crearTurno();
    }

    @Test
    void envioConPlantillaAprobadaRegistraIntentoYProveedorMessageId() {
        store.registrarPlantilla("turno_confirmacion", Proposito.CONFIRMACION, "es_AR", "APROBADA");
        Notificacion programada = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);
        assertEquals(EstadoNotificacion.PENDIENTE, programada.estado());

        assertEquals(1, dispatcher.procesar(AHORA, 10));

        Notificacion enviada = store.notificacion(programada.id()).orElseThrow();
        assertEquals(EstadoNotificacion.ENVIADA, enviada.estado());
        assertEquals(1, enviada.intentos());
        assertNotNull(enviada.proveedorMessageId());
        assertEquals("turno_confirmacion", gateway.enviados().get(0).codigoPlantilla());
        assertEquals(List.of("EXITO"), jdbcTemplate.queryForList(
                "SELECT resultado FROM notificacion_intentos WHERE notificacion_id = ?", String.class, enviada.id()));
        assertEquals(List.of("notificacion.programada", "notificacion.enviada"), tipos());
        assertEquals(0, dispatcher.procesar(AHORA.plusSeconds(3600), 10), "no se reenvía lo ya enviado");
        assertEquals(1, gateway.enviados().size());
    }

    @Test
    void plantillaRechazadaBloqueaElEnvioYAvisaAAdministracion() {
        store.registrarPlantilla("turno_recordatorio", Proposito.RECORDATORIO, "es_AR", "RECHAZADA");

        Notificacion notificacion = notificaciones.programar(turnoId, Proposito.RECORDATORIO, "+5491100000000", AHORA);

        assertEquals(EstadoNotificacion.BLOQUEADA, notificacion.estado());
        assertEquals("plantilla-no-aprobada", notificacion.ultimoError());
        assertEquals(0, dispatcher.procesar(AHORA, 10));
        assertTrue(gateway.enviados().isEmpty());
        AuditEvent aviso = auditoria.eventos().get(0);
        assertEquals("notificacion.bloqueada", aviso.tipo());
        assertEquals("administracion", aviso.metadata().get("aviso"));
        assertEquals("SYSTEM", aviso.actor().modo());
    }

    @Test
    void sinPlantillaSembradaNoSeEnviaNada() {
        Notificacion notificacion = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);

        assertEquals(EstadoNotificacion.BLOQUEADA, notificacion.estado());
        assertEquals("plantilla-inexistente", notificacion.ultimoError());
        assertTrue(gateway.enviados().isEmpty());
    }

    @Test
    void plantillaRechazadaDespuesDeProgramarSeBloqueaAlDespachar() {
        long plantilla = store.registrarPlantilla("turno_confirmacion", Proposito.CONFIRMACION, "es_AR", "APROBADA");
        Notificacion programada = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);
        store.actualizarAprobacion(plantilla, "RECHAZADA", "rechazada por Meta");

        dispatcher.procesar(AHORA, 10);

        assertEquals(EstadoNotificacion.BLOQUEADA, store.notificacion(programada.id()).orElseThrow().estado());
        assertTrue(gateway.enviados().isEmpty());
        assertTrue(tipos().contains("notificacion.bloqueada"));
    }

    @Test
    void programarDosVecesEsIdempotente() {
        store.registrarPlantilla("turno_confirmacion", Proposito.CONFIRMACION, "es_AR", "APROBADA");

        Notificacion primera = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);
        Notificacion segunda = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);

        assertEquals(primera.id(), segunda.id());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notificaciones_outbox", Integer.class));
    }

    @Test
    void erroresSeReintentanConRegistroHastaElMaximoYLuegoFalla() {
        store.registrarPlantilla("turno_confirmacion", Proposito.CONFIRMACION, "es_AR", "APROBADA");
        Notificacion programada = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);
        gateway.fallarProximos(5);

        assertEquals(1, dispatcher.procesar(AHORA, 10));
        Notificacion tras1 = store.notificacion(programada.id()).orElseThrow();
        assertEquals(EstadoNotificacion.PENDIENTE, tras1.estado());
        assertEquals(1, tras1.intentos());
        assertEquals(0, dispatcher.procesar(AHORA.plusSeconds(30), 10), "respeta la espera de reintento");

        assertEquals(1, dispatcher.procesar(AHORA.plusSeconds(61), 10));
        Notificacion tras2 = store.notificacion(programada.id()).orElseThrow();
        assertEquals(EstadoNotificacion.FALLIDA, tras2.estado());
        assertEquals(2, tras2.intentos());
        assertEquals("fallo-simulado", tras2.ultimoError());
        assertEquals(List.of(1, 2), jdbcTemplate.queryForList(
                "SELECT numero FROM notificacion_intentos WHERE notificacion_id = ? ORDER BY numero",
                Integer.class, programada.id()));
        assertTrue(tipos().contains("notificacion.fallida"));
        assertEquals(0, dispatcher.procesar(AHORA.plusSeconds(10_000), 10));
    }

    @Test
    void webhookSinFirmaOConFirmaInvalidaSeDescartaSinEfectos() {
        Notificacion enviada = enviarConfirmacion();
        byte[] cuerpo = estado(enviada.proveedorMessageId(), "delivered");

        assertThrows(WebhookRechazadoException.class, () -> webhook.recibir(null, cuerpo));
        assertThrows(WebhookRechazadoException.class, () -> webhook.recibir("sha256=" + "ab".repeat(32), cuerpo));

        assertEquals(EstadoNotificacion.ENVIADA, store.notificacion(enviada.id()).orElseThrow().estado());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM whatsapp_webhook_eventos", Integer.class));
        assertEquals(2, tipos().stream().filter("whatsapp.webhook.rechazado"::equals).count());
    }

    @Test
    void webhookDuplicadoSeAplicaUnaSolaVezYNoRetrocedeEstados() {
        Notificacion enviada = enviarConfirmacion();
        byte[] leido = estado(enviada.proveedorMessageId(), "read");
        byte[] entregado = estado(enviada.proveedorMessageId(), "delivered");

        assertEquals(1, webhook.recibir(firmar(leido), leido));
        assertEquals(0, webhook.recibir(firmar(leido), leido), "reintento del proveedor");
        assertEquals(1, webhook.recibir(firmar(entregado), entregado));

        assertEquals(EstadoNotificacion.LEIDA, store.notificacion(enviada.id()).orElseThrow().estado());
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM whatsapp_webhook_eventos", Integer.class));
    }

    @Test
    void respuestaDeBotonConfirmaLaNotificacionYSeAudita() {
        Notificacion enviada = enviarConfirmacion();
        byte[] respuesta = ("{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"id\":\"wamid.in-1\","
                + "\"type\":\"button\",\"context\":{\"id\":\"" + enviada.proveedorMessageId() + "\"},"
                + "\"button\":{\"payload\":\"confirmar\"}}]}}]}]}").getBytes(StandardCharsets.UTF_8);

        assertEquals(1, webhook.recibir(firmar(respuesta), respuesta));
        assertEquals(0, webhook.recibir(firmar(respuesta), respuesta));

        assertEquals(EstadoNotificacion.CONFIRMADA, store.notificacion(enviada.id()).orElseThrow().estado());
        assertEquals(1, tipos().stream().filter("notificacion.confirmada"::equals).count());
    }

    @Test
    void intentosYEventosDeWebhookSonAppendOnly() {
        Notificacion enviada = enviarConfirmacion();
        byte[] cuerpo = estado(enviada.proveedorMessageId(), "delivered");
        webhook.recibir(firmar(cuerpo), cuerpo);

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update("DELETE FROM notificacion_intentos"));
        assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("UPDATE whatsapp_webhook_eventos SET tipo = 'x'"));
    }

    @Test
    void restriccionesDeLaMigracion() {
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO plantillas_whatsapp (codigo_meta, proposito, idioma, estado_aprobacion) "
                        + "VALUES ('x', 'MARKETING', 'es', 'APROBADA')"));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO plantillas_whatsapp (codigo_meta, proposito, idioma, estado_aprobacion) "
                        + "VALUES ('x', 'CONFIRMACION', 'es', 'APROBADISIMA')"));
        Integer semillas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plantillas_whatsapp", Integer.class);
        assertEquals(0, semillas, "los códigos de plantilla son (TBD): sin seed");
    }

    private Notificacion enviarConfirmacion() {
        store.registrarPlantilla("turno_confirmacion", Proposito.CONFIRMACION, "es_AR", "APROBADA");
        Notificacion programada = notificaciones.programar(turnoId, Proposito.CONFIRMACION, "+5491100000000", AHORA);
        dispatcher.procesar(AHORA, 10);
        auditoria.clear();
        return store.notificacion(programada.id()).orElseThrow();
    }

    private byte[] estado(String messageId, String estado) {
        return ("{\"entry\":[{\"changes\":[{\"value\":{\"statuses\":[{\"id\":\"" + messageId
                + "\",\"status\":\"" + estado + "\"}]}}]}]}").getBytes(StandardCharsets.UTF_8);
    }

    private String firmar(byte[] cuerpo) {
        return "sha256=" + HexFormat.of().formatHex(verificador.calcular(cuerpo));
    }

    private List<String> tipos() {
        return auditoria.eventos().stream().map(AuditEvent::tipo).toList();
    }

    private long crearTurno() {
        Long paciente = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, "
                + "nombre, apellido, telefono) VALUES ('DNI', '30111222', 'Ana', 'Paz', '+5491100000000') "
                + "RETURNING id", Long.class);
        Long profesional = jdbcTemplate.queryForObject("INSERT INTO profesionales (matricula, nombre, apellido) "
                + "VALUES ('MP-NOT-1', 'Eva', 'Ruiz') RETURNING id", Long.class);
        Long sillon = jdbcTemplate.queryForObject("INSERT INTO sillones (numero, consultorio_id) "
                + "VALUES ('S-NOT-1', 1) RETURNING id", Long.class);
        Long tratamiento = jdbcTemplate.queryForObject("INSERT INTO tratamientos (codigo, nombre, duracion_minutos) "
                + "VALUES ('CTRL-NOT', 'Control', 30) RETURNING id", Long.class);
        return jdbcTemplate.queryForObject("INSERT INTO turnos (paciente_id, profesional_id, sillon_id, "
                + "tipo_tratamiento_id, inicio, fin, duracion_minutos, estado, origen) VALUES "
                + "(?, ?, ?, ?, '2026-12-11T13:00:00Z', '2026-12-11T13:30:00Z', 30, 'fixture', 'fixture') "
                + "RETURNING id", Long.class, paciente, profesional, sillon, tratamiento);
    }
}
