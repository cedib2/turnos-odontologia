package com.turnosodontologia.application.clinico;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.support.ActorConmutable;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ActorConmutable.Config.class)
class HistoriaClinicaPostgresTest extends PostgresFlywayTest {

    @Autowired
    private HistoriaClinicaService historia;

    @Autowired
    private ActorConmutable actores;

    @Autowired
    private InMemoryAuditEventRecorder auditoria;

    private long pacienteId;
    private long profesionalId;
    private long turnoId;

    @BeforeEach
    void preparar() {
        actores.usar(ActorConmutable.ODONTOLOGO);
        auditoria.clear();
        pacienteId = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, nombre, "
                + "apellido, telefono) VALUES ('DNI', '28999111', 'Luis', 'Gómez', '+5491111111111') RETURNING id",
                Long.class);
        profesionalId = jdbcTemplate.queryForObject("INSERT INTO profesionales (matricula, nombre, apellido) "
                + "VALUES ('MP-CL-1', 'Dra.', 'Sosa') RETURNING id", Long.class);
        Long sillon = jdbcTemplate.queryForObject("INSERT INTO sillones (numero, consultorio_id) VALUES ('S-CL', 1) "
                + "RETURNING id", Long.class);
        Long tratamiento = jdbcTemplate.queryForObject("INSERT INTO tratamientos (codigo, nombre, duracion_minutos) "
                + "VALUES ('CL-CTRL', 'Control', 30) RETURNING id", Long.class);
        turnoId = jdbcTemplate.queryForObject("INSERT INTO turnos (paciente_id, profesional_id, sillon_id, "
                + "tipo_tratamiento_id, inicio, fin, duracion_minutos, estado, origen) VALUES (?, ?, ?, ?, "
                + "'2026-12-14T13:00:00Z', '2026-12-14T13:30:00Z', 30, 'fixture', 'fixture') RETURNING id",
                Long.class, pacienteId, profesionalId, sillon, tratamiento);
    }

    @Test
    void catalogoFdiIncluyePiezasPermanentesYTemporales() {
        assertEquals(52, count("SELECT COUNT(*) FROM piezas_fdi"));
        assertEquals(32, count("SELECT COUNT(*) FROM piezas_fdi WHERE NOT temporal"));
        assertEquals(20, count("SELECT COUNT(*) FROM piezas_fdi WHERE temporal"));
        assertEquals(List.of(51, 55, 61, 65, 71, 75, 81, 85), jdbcTemplate.queryForList(
                "SELECT codigo FROM piezas_fdi WHERE temporal AND posicion IN (1, 5) ORDER BY codigo", Integer.class));
        assertEquals(0, count("SELECT COUNT(*) FROM piezas_fdi WHERE codigo IN (10, 19, 56, 86, 91)"));
    }

    @Test
    void odontogramaAceptaSoloCodificacionFdiConTemporales() {
        long ficha = historia.abrirFicha(pacienteId, null);

        historia.registrarOdontograma(ficha, 55, "oclusal", "caries");
        historia.registrarOdontograma(ficha, 16, null, "sano");

        assertThrows(IllegalArgumentException.class, () -> historia.registrarOdontograma(ficha, 19, null, "x"));
        assertThrows(IllegalArgumentException.class, () -> historia.registrarOdontograma(ficha, 56, null, "x"));
        assertThrows(IllegalArgumentException.class, () -> historia.registrarOdontograma(ficha, 91, null, "x"));

        HistoriaClinica vista = historia.consultar(ficha);
        assertEquals(2, vista.odontograma().size());
        assertEquals(List.of(16, 55), vista.odontograma().stream().map(HistoriaClinica.RegistroOdontograma::piezaFdi).toList());
        assertFalse(vista.odontograma().get(0).temporal());
        assertTrue(vista.odontograma().get(1).temporal());
    }

    @Test
    void registrarDeNuevoCreaVersionYConservaLaAnterior() {
        long ficha = historia.abrirFicha(pacienteId, null);

        assertEquals(1, historia.registrarOdontograma(ficha, 36, "mesial", "caries"));
        assertEquals(2, historia.registrarOdontograma(ficha, 36, "mesial", "obturado"));
        assertEquals(1, historia.registrarOdontograma(ficha, 36, "distal", "sano"));
        assertEquals(1, historia.registrarAnamnesis(ficha, Map.of("alergias", "penicilina"), "sin antecedentes"));
        assertEquals(2, historia.registrarAnamnesis(ficha, Map.of("alergias", "ninguna"), null));

        HistoriaClinica vista = historia.consultar(ficha);
        HistoriaClinica.RegistroOdontograma mesial = vista.odontograma().stream()
                .filter(r -> "mesial".equals(r.cara())).findFirst().orElseThrow();
        assertEquals("obturado", mesial.estado());
        assertEquals(2, mesial.version());
        assertEquals(2, vista.anamnesis().version());
        assertEquals("ninguna", vista.anamnesis().respuestas().get("alergias"));
        assertEquals(List.of("caries", "obturado"), jdbcTemplate.queryForList("SELECT estado FROM odontograma_registros "
                + "WHERE pieza_fdi = 36 AND cara = 'mesial' ORDER BY version", String.class));
        assertEquals(2, count("SELECT COUNT(*) FROM anamnesis"));
        assertEquals(5, vista.historiaVersion());
    }

    @Test
    void laBaseImpideSobrescribirOBorrarHistoria() {
        long ficha = historia.abrirFicha(pacienteId, null);
        historia.registrarOdontograma(ficha, 11, null, "sano");
        historia.registrarAnamnesis(ficha, Map.of(), "texto");
        historia.registrarEvolucion(ficha, profesionalId, turnoId, "control", null);
        historia.registrarConsentimiento(ficha, "tratamiento", "acepto");

        for (String sql : List.of("UPDATE odontograma_registros SET estado = 'x'", "DELETE FROM odontograma_registros",
                "UPDATE anamnesis SET texto_libre = 'x'", "UPDATE evoluciones SET texto = 'x'",
                "DELETE FROM evoluciones", "UPDATE consentimientos SET texto = 'x'", "DELETE FROM fichas_clinicas",
                "DELETE FROM piezas_fdi WHERE codigo = 11")) {
            assertThrows(DataAccessException.class, () -> jdbcTemplate.update(sql), sql);
        }
    }

    @Test
    void modificarUnaEvolucionCreaNuevaVersionEnlazada() {
        long ficha = historia.abrirFicha(pacienteId, null);
        long original = historia.registrarEvolucion(ficha, profesionalId, turnoId, "texto inicial", null);

        long corregida = historia.registrarEvolucion(ficha, profesionalId, turnoId, "texto corregido", original);

        HistoriaClinica vista = historia.consultar(ficha);
        assertEquals(1, vista.evoluciones().size());
        assertEquals(corregida, vista.evoluciones().get(0).id());
        assertEquals(2, vista.evoluciones().get(0).version());
        assertEquals(original, vista.evoluciones().get(0).reemplazaId());
        assertEquals("texto inicial", jdbcTemplate.queryForObject(
                "SELECT texto FROM evoluciones WHERE id = ?", String.class, original));
        assertThrows(IllegalStateException.class,
                () -> historia.registrarEvolucion(ficha, profesionalId, null, "rama paralela", original));
    }

    @Test
    void fichaUnicaPorPacienteYAperturaDesdeElTurno() {
        long desdeTurno = historia.abrirDesdeTurno(turnoId);
        assertEquals(desdeTurno, historia.abrirFicha(pacienteId, null));
        assertEquals(turnoId, jdbcTemplate.queryForObject(
                "SELECT turno_apertura_id FROM fichas_clinicas WHERE id = ?", Long.class, desdeTurno));

        Long otro = jdbcTemplate.queryForObject("INSERT INTO pacientes (documento_tipo, documento_numero, nombre, "
                + "apellido, telefono) VALUES ('DNI', '28999222', 'Otro', 'P', '+549') RETURNING id", Long.class);
        assertThrows(IllegalArgumentException.class, () -> historia.abrirFicha(otro, turnoId));
    }

    @Test
    void permisosPorRolSegunElSeed() {
        long ficha = historia.abrirFicha(pacienteId, null);

        actores.usar(ActorConmutable.ADMIN);
        historia.registrarOdontograma(ficha, 21, null, "sano");
        historia.consultar(ficha);

        actores.usar(ActorSimulado.recepcionista());
        historia.registrarConsentimiento(ficha, "datos", "acepto");
        assertThrows(AccesoClinicoDenegadoException.class, () -> historia.consultar(ficha));

        actores.usar(ActorConmutable.AUDITOR);
        assertThrows(AccesoClinicoDenegadoException.class, () -> historia.registrarOdontograma(ficha, 21, null, "x"));

        actores.usar(ActorSimulado.system());
        assertThrows(AccesoClinicoDenegadoException.class, () -> historia.consultar(ficha));

        actores.usar(ActorSimulado.paciente(99L, "Luis"));
        assertThrows(AccesoClinicoDenegadoException.class, () -> historia.consultar(ficha));

        assertEquals(4, auditoria.eventos().stream().filter(e -> e.tipo().equals("clinico.acceso.denegado")).count());
    }

    @Test
    void cadaCambioQuedaTrazadoEnAuditoriaSinContenidoClinico() {
        long ficha = historia.abrirFicha(pacienteId, turnoId);
        historia.registrarAnamnesis(ficha, Map.of("medicacion", "ninguna"), "libre");
        historia.registrarOdontograma(ficha, 46, "oclusal", "caries");
        historia.registrarEvolucion(ficha, profesionalId, turnoId, "detalle clínico", null);
        historia.registrarConsentimiento(ficha, "tratamiento", "texto del consentimiento");

        List<AuditEvent> eventos = auditoria.eventos();
        assertEquals(List.of("clinico.ficha.abierta", "clinico.anamnesis.registrada", "clinico.odontograma.registrado",
                "clinico.evolucion.registrada", "clinico.consentimiento.registrado"),
                eventos.stream().map(AuditEvent::tipo).toList());
        assertEquals(List.of(1, 2, 3, 4), eventos.stream().skip(1).map(e -> e.metadata().get("historiaVersion")).toList());
        for (AuditEvent evento : eventos) {
            assertEquals(ficha, evento.entidadId());
            assertEquals("Dra. Sosa", evento.actor().nombre());
            assertEquals("REAL", evento.actor().modo());
            String metadata = evento.metadata().toString();
            assertFalse(metadata.contains("detalle clínico") || metadata.contains("ninguna")
                    || metadata.contains("caries") || metadata.contains("texto del consentimiento"), metadata);
        }
        assertEquals(301L, jdbcTemplate.queryForObject("SELECT autor_id FROM evoluciones", Long.class));
    }

    @Test
    void fichaSinRegistrosDevuelveVistaVacia() {
        long ficha = historia.abrirFicha(pacienteId, null);
        HistoriaClinica vista = historia.consultar(ficha);
        assertNull(vista.anamnesis());
        assertTrue(vista.odontograma().isEmpty());
        assertEquals(0, vista.historiaVersion());
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
