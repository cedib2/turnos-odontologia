package com.turnosodontologia.export;

import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ExportOperationalConfig {

    public static final String CRON_PROPERTY = "BACKUP_CRON";
    public static final String RETENCION_PROPERTY = "EXPORT_RETENTION_YEARS";
    public static final String CLAVE_PROPERTY = "AUTH_JWT_SECRET";
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final String cron;
    private final int retencionAnios;
    private final byte[] clave;

    public ExportOperationalConfig(Environment environment) {
        this.cron = environment.getRequiredProperty(CRON_PROPERTY);
        this.retencionAnios = environment.getRequiredProperty(RETENCION_PROPERTY, Integer.class);
        String secreto = environment.getRequiredProperty(CLAVE_PROPERTY);
        try {
            this.clave = MessageDigest.getInstance("SHA-256")
                    .digest(secreto.getBytes(StandardCharsets.UTF_8));
        } catch (Exception excepcion) {
            throw new IllegalStateException("No se pudo derivar la clave AES-256", excepcion);
        }
    }

    public String cron() {
        return cron;
    }

    public int retencionAnios() {
        return retencionAnios;
    }

    public byte[] clave() {
        return clave.clone();
    }

    public ZoneId zona() {
        return ZONA;
    }

    public ZonedDateTime proximaEjecucion(Instant desde) {
        String[] campos = cron.trim().split("\\s+");
        if (campos.length < 3) {
            throw new IllegalArgumentException("BACKUP_CRON debe tener segundo, minuto y hora");
        }
        int segundo = Integer.parseInt(campos[0]);
        int minuto = Integer.parseInt(campos[1]);
        int hora = Integer.parseInt(campos[2]);
        ZonedDateTime actual = ZonedDateTime.ofInstant(desde, ZONA);
        ZonedDateTime candidata = actual.toLocalDate().atStartOfDay(ZONA)
                .withHour(hora).withMinute(minuto).withSecond(segundo).withNano(0);
        return actual.isBefore(candidata) ? candidata : candidata.plusDays(1);
    }
}
