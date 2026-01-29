package com.poc.onboarding.common.dto;

/**
 * Tipo de operación solicitada a los servicios mock
 */
public enum TipoOperacion {
    CREAR,      // Crear nuevo recurso (usuario LDAP, email, accesos, equipamiento)
    ELIMINAR,   // Eliminar/revocar recurso (compensación)
    ASIGNAR,    // Asignar recurso (equipamiento)
    REVOCAR     // Revocar acceso (sistemas, equipamiento)
}
