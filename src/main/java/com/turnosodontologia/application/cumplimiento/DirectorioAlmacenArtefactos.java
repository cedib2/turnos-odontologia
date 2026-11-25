package com.turnosodontologia.application.cumplimiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Almacén en un directorio local ({@code cumplimiento.almacen.directorio} / {@code BACKUP_DIR}).
 * Es el adaptador mínimo de B5; el destino definitivo en la nube sigue (TBD).
 */
@Component
public class DirectorioAlmacenArtefactos implements AlmacenArtefactos {

    private static final String PREFIJO = "dir:";

    private final Path directorio;

    public DirectorioAlmacenArtefactos(
            @Value("${cumplimiento.almacen.directorio:${BACKUP_DIR:}}") String directorio) {
        this.directorio = directorio == null || directorio.isBlank() ? null : Path.of(directorio);
    }

    @Override
    public boolean configurado() {
        return directorio != null;
    }

    @Override
    public String guardar(String clave, byte[] contenido) {
        exigirConfigurado();
        if (!clave.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Clave de artefacto inválida: " + clave);
        }
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(clave);
            Files.write(destino, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return PREFIJO + clave;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el artefacto en el destino", e);
        }
    }

    @Override
    public byte[] leer(String referencia) {
        exigirConfigurado();
        if (referencia == null || !referencia.startsWith(PREFIJO)) {
            throw new IllegalArgumentException("Referencia de artefacto desconocida");
        }
        Path archivo = directorio.resolve(referencia.substring(PREFIJO.length())).normalize();
        if (!archivo.startsWith(directorio.normalize())) {
            throw new IllegalArgumentException("Referencia fuera del destino configurado");
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el artefacto", e);
        }
    }

    private void exigirConfigurado() {
        if (directorio == null) {
            throw new IllegalStateException("Destino de respaldo no configurado (BACKUP_DIR, destino TBD)");
        }
    }
}
