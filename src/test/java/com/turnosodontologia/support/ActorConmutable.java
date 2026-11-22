package com.turnosodontologia.support;

import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Proveedor de actor para tests clínicos: permite simular cada rol y modo. */
public class ActorConmutable implements CurrentActorProvider {

    public static final ActorSimulado ODONTOLOGO = ActorSimulado.real(301L, "Dra. Sosa", "ODONTOLOGO", "jti-odo");
    public static final ActorSimulado AUDITOR = ActorSimulado.real(302L, "Auditor", "AUDITOR", "jti-aud");
    public static final ActorSimulado ADMIN = ActorSimulado.real(303L, "Admin", "ADMINISTRADOR", "jti-adm");

    private volatile ActorSimulado actual = ODONTOLOGO;

    public void usar(ActorSimulado actor) {
        this.actual = actor;
    }

    @Override
    public ActorSimulado current() {
        return actual;
    }

    @Override
    public ActorSimulado system() {
        return ActorSimulado.system();
    }

    /** Solo se registra con {@code @Import} explícito: sin anotación para que ningún escaneo lo recoja. */
    public static class Config {

        @Bean
        @Primary
        public ActorConmutable actorConmutable() {
            return new ActorConmutable();
        }
    }
}
