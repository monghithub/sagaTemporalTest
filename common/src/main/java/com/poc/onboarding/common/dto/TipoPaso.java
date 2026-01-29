package com.poc.onboarding.common.dto;

/**
 * Tipos de pasos en el proceso de onboarding
 */
public enum TipoPaso {
    LDAP,           // Creación de usuario en LDAP/AD
    EMAIL,          // Provisión de cuenta de correo
    SISTEMAS,       // Alta en sistemas internos (ERP, intranet, fichajes)
    EQUIPAMIENTO    // Asignación de equipamiento IT
}
