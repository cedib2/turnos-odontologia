package com.turnosodontologia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Equivale a {@code @SpringBootApplication}, pero sin {@code @SpringBootConfiguration}: los tests
 * {@code @DataJpaTest} descubren {@code TestJpaConfig} (la única {@code @SpringBootConfiguration} del
 * paquete raíz) y fallarían si encontraran dos.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)})
public class TurnosOdontologiaApplication {

    /**
     * Reloj por defecto de la aplicación. Los módulos que necesitan uno propio lo piden por nombre
     * ({@code publicClock}, {@code relojAccesoHistoria}); los tests definen el suyo y no cargan esta clase.
     */
    @Bean
    @Primary
    Clock reloj() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(TurnosOdontologiaApplication.class, args);
    }
}
