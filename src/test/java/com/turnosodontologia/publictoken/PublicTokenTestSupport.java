package com.turnosodontologia.publictoken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PublicTokenTestSupport {

    static final String EMISOR = "turnos-odontologia-public";
    static final String SEMILLA = "semilla-de-prueba-publica-2026";

    private PublicTokenTestSupport() {
    }

    static Clock reloj(Instant ahora) {
        return Clock.fixed(ahora, ZoneOffset.UTC);
    }

    static PublicTokenClaims claims(String jti, PublicTokenScope scope, Instant ahora,
                                    String turnoId, String pacienteId) {
        long emitido = ahora.getEpochSecond();
        return new PublicTokenClaims(jti, EMISOR, pacienteId, pacienteId, scope, emitido,
                emitido + scope.vigenciaSegundos(), turnoId, false);
    }

    static final class FakeMetadataStore implements PublicTokenMetadataStore {

        private final Map<String, PublicTokenClaims> registros = new HashMap<>();
        private final Map<String, Boolean> usados = new HashMap<>();
        private final Map<String, Boolean> revocados = new HashMap<>();
        private final List<String> rotados = new ArrayList<>();

        @Override
        public void registrar(PublicTokenClaims claims, String hash) {
            registros.put(claims.jti(), claims);
            usados.put(claims.jti(), false);
            revocados.put(claims.jti(), false);
        }

        @Override
        public boolean estaDisponible(String jti) {
            return Boolean.FALSE.equals(usados.get(jti)) && Boolean.FALSE.equals(revocados.get(jti));
        }

        @Override
        public boolean marcarUsado(String jti) {
            if (!estaDisponible(jti)) {
                return false;
            }
            usados.put(jti, true);
            return true;
        }

        @Override
        public boolean revocar(String jti) {
            if (!registros.containsKey(jti)) {
                return false;
            }
            revocados.put(jti, true);
            return true;
        }

        @Override
        public void rotar(String anterior, PublicTokenClaims nuevo, String hash) {
            marcarUsado(anterior);
            registrar(nuevo, hash);
            rotados.add(anterior);
        }

        Map<String, PublicTokenClaims> registros() {
            return Map.copyOf(registros);
        }

        List<String> rotados() {
            return List.copyOf(rotados);
        }

        boolean fueUsado(String jti) {
            return Boolean.TRUE.equals(usados.get(jti));
        }

        boolean fueRevocado(String jti) {
            return Boolean.TRUE.equals(revocados.get(jti));
        }
    }
}
