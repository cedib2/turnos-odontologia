package com.turnosodontologia.publictoken;

public record PublicTokenClaims(
        String jti,
        String iss,
        String sub,
        String pacienteId,
        PublicTokenScope scope,
        long iat,
        long exp,
        String turnoId,
        boolean identidadVerificada) {
}
