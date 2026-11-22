package com.turnosodontologia.publictoken;

public class PublicAgendaConsumption {

    private final PublicTokenService tokens;

    public PublicAgendaConsumption(PublicTokenService tokens) {
        this.tokens = tokens;
    }

    public Resultado validarDesdePath(String valor) {
        return validar(valor);
    }

    public Resultado validarDesdeQuery(String valor) {
        return validar(valor);
    }

    private Resultado validar(String valor) {
        PublicTokenService.Validacion validacion = tokens.validar(valor);
        if (!validacion.permitido()) {
            return new Resultado(false, validacion.motivo(), null);
        }
        PublicTokenScope scope = validacion.claims().scope();
        if (scope != PublicTokenScope.RESERVAR && scope != PublicTokenScope.HISTORIA) {
            return new Resultado(false, "scope_no_permitido", null);
        }
        return new Resultado(true, validacion.motivo(), scope);
    }

    public record Resultado(boolean permitido, String motivo, PublicTokenScope scope) {
    }
}
