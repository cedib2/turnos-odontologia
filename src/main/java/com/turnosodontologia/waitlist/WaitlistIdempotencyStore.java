package com.turnosodontologia.waitlist;

import com.turnosodontologia.publictoken.PublicIdempotencyStore;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Idempotencia de la adjudicación (W-7): reutiliza {@code public_idempotencia}
 * de V004 sin cambios de esquema. La clave es el turno liberado y el hash
 * SHA-256 lo calcula {@link PublicIdempotencyStore}.
 */
@Component
public class WaitlistIdempotencyStore {

    public static final String OPERACION = "waitlist.adjudicar";

    private final PublicIdempotencyStore store;

    public WaitlistIdempotencyStore(DataSource dataSource) {
        this.store = new PublicIdempotencyStore(dataSource);
    }

    public boolean registrar(Long turnoId) {
        return store.registrar(OPERACION, String.valueOf(turnoId));
    }

    public boolean esRepeticion(Long turnoId) {
        return store.esRepeticion(OPERACION, String.valueOf(turnoId));
    }
}
