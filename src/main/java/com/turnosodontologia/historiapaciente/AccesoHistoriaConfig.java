package com.turnosodontologia.historiapaciente;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * Cableado del acceso del paciente a su historia: verificador de identidad según perfil.
 * El {@code PublicTokenService} de C-06 lo provee {@code PublicBookingConfig}.
 */
@Configuration
@Import(AccesoHistoriaPacienteService.class)
public class AccesoHistoriaConfig {

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
