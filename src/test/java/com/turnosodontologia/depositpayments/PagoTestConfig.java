package com.turnosodontologia.depositpayments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Proveedor de pagos simulado para los tests de integración de C-15. */
public class PagoTestConfig {

    @Bean
    @Primary
    MercadoPagoPort mercadoPagoPortStub() {
        return new MercadoPagoStub();
    }

    public static final class MercadoPagoStub implements MercadoPagoPort {

        private final List<Preferencia> preferencias = new CopyOnWriteArrayList<>();
        private final List<Reembolso> reembolsos = new CopyOnWriteArrayList<>();

        @Override
        public String crearPreferencia(String externalReference, BigDecimal monto,
                                       String idempotencyKey) {
            preferencias.add(new Preferencia(externalReference, monto, idempotencyKey));
            return "pref-" + externalReference;
        }

        @Override
        public void reembolsar(String paymentId, String externalReference, BigDecimal monto) {
            reembolsos.add(new Reembolso(paymentId, externalReference, monto));
        }

        public List<Preferencia> preferencias() {
            return List.copyOf(preferencias);
        }

        public List<Reembolso> reembolsos() {
            return List.copyOf(reembolsos);
        }

        public void clear() {
            preferencias.clear();
            reembolsos.clear();
        }

        public record Preferencia(String externalReference, BigDecimal monto, String idempotencyKey) {
        }

        public record Reembolso(String paymentId, String externalReference, BigDecimal monto) {
        }
    }
}
