package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.application.cumplimiento.CifradorArtefactos;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CifradorArtefactosTest {

    private static final String CLAVE_A = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String CLAVE_B = Base64.getEncoder().encodeToString("b".repeat(32).getBytes());

    @Test
    void cifraYDescifraConLaMismaClave() {
        CifradorArtefactos cifrador = new CifradorArtefactos(CLAVE_A);
        byte[] plano = "paciente DOC-123".getBytes(StandardCharsets.UTF_8);

        byte[] cifrado = cifrador.cifrar(plano);

        assertFalse(new String(cifrado, StandardCharsets.ISO_8859_1).contains("DOC-123"));
        assertArrayEquals(plano, cifrador.descifrar(cifrado));
    }

    @Test
    void otraClaveNoDescifra() {
        byte[] cifrado = new CifradorArtefactos(CLAVE_A).cifrar("dato".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> new CifradorArtefactos(CLAVE_B).descifrar(cifrado));
    }

    @Test
    void sinClaveNoCifraYClaveCortaSeRechaza() {
        assertThrows(IllegalStateException.class, () -> new CifradorArtefactos("").cifrar(new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> new CifradorArtefactos(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
