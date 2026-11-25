package com.turnosodontologia.waitlist;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registro de propiedades de C-19 (W-12). */
@Configuration
@EnableConfigurationProperties(WaitlistProperties.class)
public class WaitlistConfig {
}
