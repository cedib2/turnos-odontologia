package com.turnosodontologia.export;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalBackupStorage implements BackupStoragePort {

    private static final int LONGITUD_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, BackupObjeto>> objetos =
            new ConcurrentHashMap<>();

    public LocalBackupStorage(byte[] clave) {
        this.clave = new SecretKeySpec(clave, "AES");
    }

    @Override
    public void guardar(String tenantId, String artefactoId, byte[] contenido) {
        guardar(tenantId, artefactoId, contenido, Instant.now());
    }

    public void guardar(String tenantId, String artefactoId, byte[] contenido, Instant guardadoEn) {
        byte[] iv = new byte[LONGITUD_IV];
        aleatorio.nextBytes(iv);
        byte[] cifrado;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            cifrado = cipher.doFinal(contenido);
        } catch (Exception excepcion) {
            throw new IllegalStateException("No se pudo cifrar el respaldo", excepcion);
        }
        byte[] empaquetado = new byte[iv.length + cifrado.length];
        System.arraycopy(iv, 0, empaquetado, 0, iv.length);
        System.arraycopy(cifrado, 0, empaquetado, iv.length, cifrado.length);
        tenant(tenantId).put(artefactoId, new BackupObjeto(tenantId, artefactoId, empaquetado,
                sha256(contenido), guardadoEn, false));
    }

    @Override
    public byte[] leer(String tenantId, String artefactoId) {
        BackupObjeto objeto = objeto(tenantId, artefactoId);
        byte[] empaquetado = objeto.contenidoCifrado();
        byte[] iv = new byte[LONGITUD_IV];
        byte[] cifrado = new byte[empaquetado.length - LONGITUD_IV];
        System.arraycopy(empaquetado, 0, iv, 0, LONGITUD_IV);
        System.arraycopy(empaquetado, LONGITUD_IV, cifrado, 0, cifrado.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] contenido = cipher.doFinal(cifrado);
            if (!sha256(contenido).equals(objeto.sha256())) {
                throw new IllegalStateException("SHA-256 del respaldo no coincide");
            }
            return contenido;
        } catch (IllegalStateException excepcion) {
            throw excepcion;
        } catch (Exception excepcion) {
            throw new IllegalStateException("No se pudo descifrar el respaldo", excepcion);
        }
    }

    @Override
    public List<String> listar(String tenantId) {
        return new ArrayList<>(tenant(tenantId).keySet());
    }

    public List<String> tenantIds() {
        return new ArrayList<>(objetos.keySet());
    }

    public BackupObjeto objeto(String tenantId, String artefactoId) {
        BackupObjeto objeto = tenant(tenantId).get(artefactoId);
        if (objeto == null) {
            throw new NoSuchElementException("Respaldo inexistente: " + tenantId + "/" + artefactoId);
        }
        return objeto;
    }

    public void marcarExpiradoLogico(String tenantId, String artefactoId) {
        tenant(tenantId).computeIfPresent(artefactoId, (id, objeto) -> new BackupObjeto(
                objeto.tenantId(), objeto.artefactoId(), objeto.contenidoCifrado(),
                objeto.sha256(), objeto.guardadoEn(), true));
    }

    private ConcurrentHashMap<String, BackupObjeto> tenant(String tenantId) {
        return objetos.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>());
    }

    private String sha256(byte[] contenido) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(contenido);
            StringBuilder texto = new StringBuilder();
            for (byte b : resumen) {
                texto.append(String.format("%02x", b));
            }
            return texto.toString();
        } catch (Exception excepcion) {
            throw new IllegalStateException("No se pudo calcular SHA-256", excepcion);
        }
    }
}
