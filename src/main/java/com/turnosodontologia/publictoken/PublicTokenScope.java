package com.turnosodontologia.publictoken;

public enum PublicTokenScope {

    RESERVAR(3600L),
    GESTIONAR(604800L),
    HISTORIA(86400L);

    private final long vigenciaSegundos;

    PublicTokenScope(long vigenciaSegundos) {
        this.vigenciaSegundos = vigenciaSegundos;
    }

    public long vigenciaSegundos() {
        return vigenciaSegundos;
    }

    public static PublicTokenScope desde(String valor) {
        for (PublicTokenScope scope : values()) {
            if (scope.name().equals(valor)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("scope_no_conocido");
    }
}
