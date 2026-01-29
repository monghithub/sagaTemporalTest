package com.poc.onboarding.common.dto;

/**
 * Estados del proceso de onboarding
 */
public enum EstadoProceso {
    INICIADO,       // Proceso recién creado
    EN_PROGRESO,    // En ejecución, esperando aprobaciones
    COMPLETADO,     // Todos los pasos completados exitosamente
    ROLLBACK,       // Compensaciones ejecutadas
    ERROR           // Error técnico en el proceso
}
