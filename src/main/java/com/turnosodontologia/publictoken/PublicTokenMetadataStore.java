package com.turnosodontologia.publictoken;

public interface PublicTokenMetadataStore {

    void registrar(PublicTokenClaims claims, String hash);

    boolean estaDisponible(String jti);

    boolean marcarUsado(String jti);

    boolean revocar(String jti);

    void rotar(String jtiAnterior, PublicTokenClaims nuevo, String hashNuevo);
}
