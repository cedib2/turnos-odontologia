package com.turnosodontologia.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final long ACCESS_MINUTES = 15;

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] signingKey;
    private final String issuer;

    public JwtService(@Value("${AUTH_JWT_SECRET}") String secret,
                      @Value("${AUTH_JWT_ISSUER:turnos-odontologia}") String issuer) {
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
    }

    public String emitir(long usuarioId, String rol, String jti, Instant ahora) {
        try {
            String encodedHeader = encode(mapper.writeValueAsString(
                    Map.of("alg", "HS256", "typ", "JWT")));
            String encodedPayload = encode(mapper.writeValueAsString(Map.of(
                    "sub", String.valueOf(usuarioId),
                    "rol", rol,
                    "jti", jti,
                    "iss", issuer,
                    "iat", ahora.getEpochSecond(),
                    "exp", ahora.plusSeconds(ACCESS_MINUTES * 60).getEpochSecond())));
            String unsigned = encodedHeader + "." + encodedPayload;
            return unsigned + "." + encode(sign(unsigned));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public AccessClaims verificar(String value) {
        if (value == null) {
            throw new InvalidAccessTokenException();
        }
        String[] partes = value.split("\\.", -1);
        if (partes.length != 3) {
            throw new InvalidAccessTokenException();
        }
        String unsigned = partes[0] + "." + partes[1];
        byte[] firma;
        try {
            firma = decode(partes[2]);
        } catch (IllegalArgumentException e) {
            throw new InvalidAccessTokenException();
        }
        if (!MessageDigest.isEqual(sign(unsigned), firma)) {
            throw new InvalidAccessTokenException();
        }
        try {
            JsonNode payload = mapper.readTree(decode(partes[1]));
            long exp = payload.path("exp").asLong();
            if (exp <= Instant.now().getEpochSecond()) {
                throw new InvalidAccessTokenException();
            }
            if (!issuer.equals(payload.path("iss").asText())) {
                throw new InvalidAccessTokenException();
            }
            String jti = payload.path("jti").asText();
            if (jti.isEmpty()) {
                throw new InvalidAccessTokenException();
            }
            return new AccessClaims(Long.parseLong(payload.path("sub").asText()),
                    payload.path("rol").asText(), jti, Instant.ofEpochSecond(exp));
        } catch (InvalidAccessTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidAccessTokenException();
        }
    }

    private byte[] sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, ALGORITHM));
            return mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public record AccessClaims(long usuarioId, String rol, String jti, Instant exp) {
    }
}
