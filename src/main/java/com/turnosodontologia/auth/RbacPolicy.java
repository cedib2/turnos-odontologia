package com.turnosodontologia.auth;

/**
 * Política RBAC de una superficie de staff protegida por {@link RbacAccessFilter}: qué rutas cubre y qué
 * rol puede invocar cada método sobre cada ruta, según la matriz {@code rol_permiso}.
 */
public interface RbacPolicy {

    /** Patrones de ruta de la cadena de seguridad ({@code securityMatcher}). */
    String[] patrones();

    /** Si la ruta pertenece a esta superficie. */
    boolean cubre(String ruta);

    /** Si el rol puede invocar el método sobre la ruta. Falla cerrado: sin regla, no permite. */
    boolean permite(String codigoRol, String metodo, String ruta);
}
