package com.turnosodontologia.auth;

public interface PasswordHasher {

    String hash(String rawValue);

    boolean matches(String rawValue, String storedHash);

    String dummyVerificationHash();
}
