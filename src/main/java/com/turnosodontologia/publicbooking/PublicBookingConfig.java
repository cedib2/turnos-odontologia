package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.ConfiguracionSchedulingService;
import com.turnosodontologia.publictoken.JdbcPublicTokenMetadataStore;
import com.turnosodontologia.publictoken.PublicIdempotencyStore;
import com.turnosodontologia.publictoken.PublicResponseHeaders;
import com.turnosodontologia.publictoken.PublicTokenMetadataStore;
import com.turnosodontologia.publictoken.PublicTokenSanitizer;
import com.turnosodontologia.publictoken.PublicTokenService;
import com.turnosodontologia.publictoken.PublicTokenSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
public class PublicBookingConfig {

    @Bean
    public Clock publicClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PublicTokenSigner publicTokenSigner(
            @Value("${PUBLIC_TOKEN_SECRET:}") String secreto,
            @Value("${public.token.issuer:turnos-odontologia-public}") String emisor) {
        String semilla = secreto == null || secreto.isBlank()
                ? UUID.randomUUID().toString()
                : secreto;
        return new PublicTokenSigner(semilla, emisor);
    }

    @Bean
    public PublicTokenMetadataStore publicTokenMetadataStore(DataSource dataSource) {
        return new JdbcPublicTokenMetadataStore(dataSource);
    }

    @Bean
    public PublicIdempotencyStore publicIdempotencyStore(DataSource dataSource) {
        return new PublicIdempotencyStore(dataSource);
    }

    @Bean
    public PublicTokenService publicTokenService(PublicTokenSigner firmador,
                                                 PublicTokenMetadataStore metadatos,
                                                 AuditEventPublisher eventos,
                                                 Clock reloj) {
        return new PublicTokenService(firmador, metadatos, eventos, reloj, new PublicTokenSanitizer());
    }

    @Bean
    public PublicResponseHeaders publicResponseHeaders(
            @Value("${public.surface.prefixes:}") String prefijos) {
        List<String> lista = Arrays.stream(prefijos.split(","))
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .toList();
        return new PublicResponseHeaders(lista);
    }

    @Bean
    public PublicTokenGuard publicTokenGuard(PublicTokenService tokens) {
        return new PublicTokenGuard(tokens);
    }

    @Bean
    public VentanaHabilesService ventanaHabilesService(ConfiguracionSchedulingService configuracion,
                                                       Clock reloj) {
        return new VentanaHabilesService(configuracion, reloj);
    }

    @Bean
    public NotificacionPublicaPort notificacionPublicaPort() {
        return new NotificacionSimulada();
    }
}
