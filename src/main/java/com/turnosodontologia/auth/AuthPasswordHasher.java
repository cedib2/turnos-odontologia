package com.turnosodontologia.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthPasswordHasher implements PasswordHasher {

    private static final String DUMMY_RAW_CREDENTIAL = "verificacion-sin-usuario";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String dummyHash;

    public AuthPasswordHasher() {
        this.dummyHash = encoder.encode(DUMMY_RAW_CREDENTIAL);
    }

    @Override
    public String hash(String rawValue) {
        return encoder.encode(rawValue);
    }

    @Override
    public boolean matches(String rawValue, String storedHash) {
        return encoder.matches(rawValue, storedHash);
    }

    @Override
    public String dummyVerificationHash() {
        return dummyHash;
    }
}
