package com.turnosodontologia.historiapaciente;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.publictoken.JdbcPublicTokenMetadataStore;
import com.turnosodontologia.publictoken.PublicTokenService;
import com.turnosodontologia.publictoken.PublicTokenSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Cableado del acceso del paciente a su historia: servicio de tokens de C-06 con
 * {@code PUBLIC_TOKEN_SECRET} y verificador de identidad según perfil.
 */
@Configuration
@Import(AccesoHistoriaPacienteService.class)
public class AccesoHistoriaConfig {

    @Bean
    PublicTokenService publicTokenService(@Value("${PUBLIC_TOKEN_SECRET}") String secreto,
                                          @Value("${PUBLIC_TOKEN_ISSUER:turnos-odontologia-public}") String emisor,
                                          DataSource dataSource, AuditEventPublisher auditoria) {
        return new PublicTokenService(new PublicTokenSigner(secreto, emisor),
                new JdbcPublicTokenMetadataStore(dataSource), auditoria, Clock.systemUTC());
    }

    @Bean
    @Profile({"dev", "test"})
    SimulatedVerificadorIdentidad verificadorIdentidadSimulado() {
        return new SimulatedVerificadorIdentidad();
    }

    @Bean
    @Profile("!dev & !test")
    VerificadorIdentidadNoConfigurado verificadorIdentidadNoConfigurado() {
        return new VerificadorIdentidadNoConfigurado();
    }

    @Bean
    Clock relojAccesoHistoria() {
        return Clock.systemUTC();
    }
}
