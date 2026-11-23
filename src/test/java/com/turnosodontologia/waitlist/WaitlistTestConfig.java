package com.turnosodontologia.waitlist;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Reloj fijo para que ningún test de C-19 dependa del reloj real. */
@TestConfiguration
public class WaitlistTestConfig {

    public static final Instant AHORA = Instant.parse("2026-12-10T12:00:00Z");

    @Bean
    @Primary
    Clock waitlistFixedClock() {
        return Clock.fixed(AHORA, ZoneOffset.UTC);
    }
}
