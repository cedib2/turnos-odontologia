package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.infrastructure.persistence.ComprobanteRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** AR-9: la clave, el comprobante, el estado y el evento se revierten juntos. */
@Import(FacturacionFailingAuditConfig.class)
class FacturacionRollbackTest extends PostgresFlywayTest {

    @Autowired
    private ComprobanteService servicio;

    @Autowired
    private SeniaRepository senias;

    @Autowired
    private ComprobanteRepository comprobantes;

    @Test
    void siFallaLaAuditoriaNoQuedanClaveNiComprobanteNiCambioEnLaSenia() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Senia senia = new Senia(null, null, new BigDecimal("100.00"),
                "senia-rollback-c16-" + sufijo, "idem-rollback-c16-" + sufijo);
        senia.aplicarEstado(Senia.APROBADO, java.time.Instant.now());
        Senia guardada = senias.saveAndFlush(senia);

        assertThrows(IllegalStateException.class,
                () -> servicio.emitir(guardada.getId(), ActorSimulado.recepcionista()));

        assertEquals(0, comprobantes.count());
        Integer claves = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'factura.emitir'",
                Integer.class);
        assertEquals(0, claves);
        Integer filas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log", Integer.class);
        assertEquals(0, filas);
        assertEquals(Senia.APROBADO, senias.findById(guardada.getId()).orElseThrow().getEstado());
    }
}
