package com.turnosodontologia.export;

import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupStoragePortTest {

    @Test
    void aislamientoPorTenant() {
        LocalBackupStorage storage = new LocalBackupStorage(clave());

        storage.guardar("tenant-1", "compartido", "uno".getBytes(StandardCharsets.UTF_8));
        storage.guardar("tenant-2", "compartido", "dos".getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> assertEquals("uno", new String(
                        storage.leer("tenant-1", "compartido"), StandardCharsets.UTF_8)),
                () -> assertEquals("dos", new String(
                        storage.leer("tenant-2", "compartido"), StandardCharsets.UTF_8)),
                () -> assertTrue(storage.listar("tenant-1").contains("compartido")),
                () -> assertTrue(storage.listar("tenant-3").isEmpty())
        );
    }

    @Test
    void cifradoAes256YSha256() throws Exception {
        LocalBackupStorage storage = new LocalBackupStorage(clave());
        byte[] contenido = "secreto clínico".getBytes(StandardCharsets.UTF_8);

        storage.guardar("tenant-1", "cifrado", contenido);
        BackupObjeto objeto = storage.objeto("tenant-1", "cifrado");

        assertAll(
                () -> assertFalse(new String(objeto.contenidoCifrado(), StandardCharsets.ISO_8859_1)
                        .contains("secreto clínico")),
                () -> assertEquals(sha256(contenido), objeto.sha256()),
                () -> assertArrayEquals(contenido, storage.leer("tenant-1", "cifrado"))
        );
    }

    @Test
    void destinoLocalYContratoObjectStorage() {
        LocalBackupStorage storage = new LocalBackupStorage(clave());
        storage.guardar("tenant-1", "respaldo-1", "respaldo".getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> assertTrue(BackupStoragePort.class.isAssignableFrom(LocalBackupStorage.class)),
                () -> assertEquals("respaldo", new String(
                        storage.leer("tenant-1", "respaldo-1"), StandardCharsets.UTF_8)),
                () -> assertTrue(storage.listar("tenant-1").contains("respaldo-1"))
        );
    }

    @Test
    void sinSubprocesadoresNoDeclarados() throws Exception {
        String codigo;
        try (var caminos = Files.walk(Path.of("src/main/java"))) {
            codigo = caminos.filter(Files::isRegularFile)
                    .map(archivo -> {
                        try {
                            return Files.readString(archivo);
                        } catch (Exception excepcion) {
                            throw new IllegalStateException(excepcion);
                        }
                    })
                    .reduce("", String::concat);
        }

        assertAll(
                () -> assertFalse(codigo.contains("com.amazonaws")),
                () -> assertFalse(codigo.contains("com.google.cloud")),
                () -> assertFalse(codigo.contains("azure"))
        );
    }

    private String sha256(byte[] contenido) throws Exception {
        byte[] resumen = MessageDigest.getInstance("SHA-256").digest(contenido);
        StringBuilder texto = new StringBuilder();
        for (byte b : resumen) {
            texto.append(String.format("%02x", b));
        }
        return texto.toString();
    }

    private byte[] clave() {
        try {
            KeyGenerator generador = KeyGenerator.getInstance("AES");
            generador.init(256);
            return generador.generateKey().getEncoded();
        } catch (Exception excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
