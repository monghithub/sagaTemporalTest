package com.poc.onboarding.common.dto;

/**
 * Estados posibles de una petición en los servicios mock
 */
public enum EstadoPeticion {
    PENDIENTE,    // Esperando aprobación manual
    APROBADA,     // Aprobada por el operador
    DENEGADA,     // Denegada por el operador
    COMPENSADA    // Revertida por compensación del Saga
}
