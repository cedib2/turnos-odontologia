package com.turnosodontologia.application.cumplimiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifrado en reposo de exportaciones y respaldos (RN-SE-05) con AES-256-GCM.
 * La clave llega por configuración ({@code BACKUP_ENCRYPTION_KEY}, Base64 de 32 bytes); sin clave no se
 * escribe ningún artefacto: la gestión de la clave (KMS, rotación) sigue (TBD, B6).
 */
@Component
public class CifradorArtefactos {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] CABECERA = {'T', 'O', 'C', '1'};

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public CifradorArtefactos(@Value("${cumplimiento.cifrado.clave:${BACKUP_ENCRYPTION_KEY:}}") String claveBase64) {
        this.clave = parsear(claveBase64);
    }

    public boolean configurado() {
        return clave != null;
    }

    public byte[] cifrar(byte[] plano) {
        exigirClave();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(plano);
            return ByteBuffer.allocate(CABECERA.length + iv.length + cifrado.length)
                    .put(CABECERA).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el artefacto", e);
        }
    }

    public byte[] descifrar(byte[] contenido) {
        exigirClave();
        if (contenido == null || contenido.length <= CABECERA.length + IV_BYTES
                || !Arrays.equals(Arrays.copyOf(contenido, CABECERA.length), CABECERA)) {
            throw new IllegalStateException("Artefacto sin cabecera de cifrado reconocida");
        }
        try {
            byte[] iv = Arrays.copyOfRange(contenido, CABECERA.length, CABECERA.length + IV_BYTES);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(contenido, CABECERA.length + IV_BYTES,
                    contenido.length - CABECERA.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Artefacto alterado o clave incorrecta", e);
        }
    }

    private void exigirClave() {
        if (clave == null) {
            throw new IllegalStateException(
                    "Clave de cifrado en reposo no configurada (BACKUP_ENCRYPTION_KEY, RN-SE-05)");
        }
    }

    private static SecretKeySpec parsear(String claveBase64) {
        if (claveBase64 == null || claveBase64.isBlank()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(claveBase64.trim());
        if (bytes.length != 32) {
            throw new IllegalArgumentException("BACKUP_ENCRYPTION_KEY debe ser Base64 de 32 bytes (AES-256)");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
