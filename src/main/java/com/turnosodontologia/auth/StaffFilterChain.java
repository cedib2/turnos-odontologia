package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Cadena de seguridad común a las superficies de staff: sesión de C-04, CSRF de doble envío y RBAC de la
 * {@link RbacPolicy} recibida, sin login de formulario ni sesión HTTP.
 */
final class StaffFilterChain {

    private StaffFilterChain() {
    }

    static SecurityFilterChain construir(HttpSecurity http, RbacPolicy politica, AccessAuthenticator acceso,
                                         UsuarioRepository usuarios, CsrfTokenStore csrf,
                                         String sessionCookie, String csrfCookie) throws Exception {
        RbacAccessFilter filtro = new RbacAccessFilter(acceso, usuarios, politica, csrf, sessionCookie, csrfCookie);
        return http
                .securityMatcher(politica.patrones())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(filtro, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .build();
    }
}
