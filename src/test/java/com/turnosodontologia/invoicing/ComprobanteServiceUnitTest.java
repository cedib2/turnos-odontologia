package com.turnosodontologia.invoicing;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.financiero.Comprobante;
import com.turnosodontologia.infrastructure.persistence.ComprobanteRepository;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AR-10: una referencia de seña inexistente no crea entidades ni auditoría. */
class ComprobanteServiceUnitTest {

    private final ComprobanteRepository comprobantes = mock(ComprobanteRepository.class);
    private final SeniaRepository senias = mock(SeniaRepository.class);
    private final PresupuestoRepository presupuestos = mock(PresupuestoRepository.class);
    private final AuditEventPublisher auditoria = mock(AuditEventPublisher.class);
    private final FacturacionPort facturacion = mock(FacturacionPort.class);
    private final AvisoRecepcionPort avisos = mock(AvisoRecepcionPort.class);
    private final FacturacionIdempotencyStore idempotencia = mock(FacturacionIdempotencyStore.class);

    private final ComprobanteService servicio = new ComprobanteService(comprobantes, senias,
            presupuestos, auditoria, facturacion, avisos, idempotencia, "C", "0001");

    @Test
    void conciliarConSeniaInexistenteNoCreaEntidadesNiEvento() {
        Comprobante comprobante = new Comprobante(555L, null, null,
                new BigDecimal("100.00"), "Seña odontológica");
        ReflectionTestUtils.setField(comprobante, "id", 1L);
        comprobante.emitir("CAE-TEST-1", Instant.now());
        when(comprobantes.findById(1L)).thenReturn(Optional.of(comprobante));
        when(senias.findById(555L)).thenReturn(Optional.empty());

        ConciliacionResponse resultado = servicio.conciliar(1L);

        assertFalse(resultado.conciliado());
        assertEquals("senia_inexistente", resultado.motivo());
        assertFalse(comprobante.isConciliado());
        verify(comprobantes, never()).saveAndFlush(any());
        verify(auditoria, never()).publish(any(AuditEvent.class));
        verify(senias, never()).saveAndFlush(any());
    }
}
