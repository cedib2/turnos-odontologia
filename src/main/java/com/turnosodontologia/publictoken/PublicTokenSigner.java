package com.turnosodontologia.publictoken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class PublicTokenSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    private final ObjectMapper json = new ObjectMapper();
    private final byte[] semilla;
    private final String emisorEsperado;

    public PublicTokenSigner(String semilla, String emisorEsperado) {
        this.semilla = semilla.getBytes(StandardCharsets.UTF_8);
        this.emisorEsperado = emisorEsperado;
    }

    public String emitir(PublicTokenClaims claims) {
        Map<String, Object> cabecera = new LinkedHashMap<>();
        cabecera.put("alg", ALGORITHM);
        cabecera.put("typ", "public");
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("jti", claims.jti());
        datos.put("iss", claims.iss());
        datos.put("sub", claims.sub());
        datos.put("pacienteId", claims.pacienteId());
        datos.put("scope", claims.scope().name());
        datos.put("iat", claims.iat());
        datos.put("exp", claims.exp());
        datos.put("identidadVerificada", claims.identidadVerificada());
        if (claims.turnoId() != null) {
            datos.put("turnoId", claims.turnoId());
        }
        String firmable = codificar(cabecera) + "." + codificar(datos);
        return firmable + "." + codificar(firma(firmable.getBytes(StandardCharsets.US_ASCII)));
    }

    public PublicTokenClaims verificar(String valor) {
        String[] partes = valor.split("\\.", -1);
        if (partes.length != 3) {
            throw new IllegalArgumentException("estructura_no_valida");
        }
        try {
            Map<?, ?> datos = json.readValue(DECODIFICADOR.decode(partes[1]), Map.class);
            return new PublicTokenClaims(
                    (String) datos.get("jti"),
                    (String) datos.get("iss"),
                    (String) datos.get("sub"),
                    (String) datos.get("pacienteId"),
                    PublicTokenScope.desde((String) datos.get("scope")),
                    ((Number) datos.get("iat")).longValue(),
                    ((Number) datos.get("exp")).longValue(),
                    (String) datos.get("turnoId"),
                    Boolean.TRUE.equals(datos.get("identidadVerificada")));
        } catch (IOException e) {
            throw new IllegalArgumentException("estructura_no_valida", e);
        }
    }

    public boolean firmaValida(String valor) {
        String[] partes = valor.split("\\.", -1);
        if (partes.length != 3) {
            return false;
        }
        try {
            byte[] recibida = DECODIFICADOR.decode(partes[2]);
            byte[] esperada = firma((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
            return MessageDigest.isEqual(recibida, esperada);
        } catch (Exception e) {
            return false;
        }
    }

    public String emisor() {
        return emisorEsperado;
    }

    private String codificar(Map<String, Object> datos) {
        try {
            return CODIFICADOR.encodeToString(json.writeValueAsBytes(datos));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("estructura_no_valida", e);
        }
    }

    private String codificar(byte[] datos) {
        return CODIFICADOR.encodeToString(datos);
    }

    private byte[] firma(byte[] firmable) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(semilla, ALGORITHM));
            return mac.doFinal(firmable);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
