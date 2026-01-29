-- =============================================================================
-- Script de inicialización de bases de datos
-- Ejecutado automáticamente por MySQL al arrancar el contenedor
-- =============================================================================

-- Base de datos para App Central (aplicación principal)
CREATE DATABASE IF NOT EXISTS onboarding_central CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Base de datos para Service LDAP
CREATE DATABASE IF NOT EXISTS onboarding_ldap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Base de datos para Service Email
CREATE DATABASE IF NOT EXISTS onboarding_email CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Base de datos para Service Sistemas
CREATE DATABASE IF NOT EXISTS onboarding_sistemas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Base de datos para Service Equipamiento
CREATE DATABASE IF NOT EXISTS onboarding_equip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================================================
-- Tablas para App Central
-- =============================================================================
USE onboarding_central;

CREATE TABLE IF NOT EXISTS proceso_onboarding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id VARCHAR(100) NOT NULL UNIQUE,
    run_id VARCHAR(100),
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_email VARCHAR(100) NOT NULL,
    empleado_departamento VARCHAR(100),
    empleado_puesto VARCHAR(100),
    estado VARCHAR(20) NOT NULL COMMENT 'INICIADO, EN_PROGRESO, COMPLETADO, ROLLBACK, ERROR',
    paso_actual VARCHAR(20) COMMENT 'LDAP, EMAIL, SISTEMAS, EQUIPAMIENTO',
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paso_proceso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proceso_id BIGINT NOT NULL,
    tipo_paso VARCHAR(20) NOT NULL COMMENT 'LDAP, EMAIL, SISTEMAS, EQUIPAMIENTO',
    estado VARCHAR(20) NOT NULL COMMENT 'PENDIENTE, APROBADO, DENEGADO, COMPENSADO, ERROR',
    peticion_id VARCHAR(100),
    datos_entrada JSON,
    datos_salida JSON,
    mensaje_error TEXT,
    fecha_inicio TIMESTAMP NULL,
    fecha_fin TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (proceso_id) REFERENCES proceso_onboarding(id) ON DELETE CASCADE,
    INDEX idx_proceso_id (proceso_id),
    INDEX idx_peticion_id (peticion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Tablas para Service LDAP
-- =============================================================================
USE onboarding_ldap;

CREATE TABLE IF NOT EXISTS peticion_ldap (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    peticion_id VARCHAR(100) NOT NULL UNIQUE,
    workflow_id VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_email VARCHAR(100) NOT NULL,
    username_generado VARCHAR(50),
    estado VARCHAR(20) NOT NULL COMMENT 'PENDIENTE, APROBADA, DENEGADA',
    tipo_operacion VARCHAR(20) NOT NULL COMMENT 'CREAR, ELIMINAR',
    motivo_denegacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_procesamiento TIMESTAMP NULL,
    procesado_por VARCHAR(100),
    INDEX idx_peticion_id (peticion_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Tablas para Service Email
-- =============================================================================
USE onboarding_email;

CREATE TABLE IF NOT EXISTS peticion_email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    peticion_id VARCHAR(100) NOT NULL UNIQUE,
    workflow_id VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_email VARCHAR(100) NOT NULL,
    email_corporativo VARCHAR(100),
    estado VARCHAR(20) NOT NULL COMMENT 'PENDIENTE, APROBADA, DENEGADA',
    tipo_operacion VARCHAR(20) NOT NULL COMMENT 'CREAR, ELIMINAR',
    motivo_denegacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_procesamiento TIMESTAMP NULL,
    procesado_por VARCHAR(100),
    INDEX idx_peticion_id (peticion_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Tablas para Service Sistemas
-- =============================================================================
USE onboarding_sistemas;

CREATE TABLE IF NOT EXISTS peticion_sistemas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    peticion_id VARCHAR(100) NOT NULL UNIQUE,
    workflow_id VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_email VARCHAR(100) NOT NULL,
    sistemas_asignados JSON COMMENT 'Lista de sistemas: ERP, Intranet, Fichajes',
    estado VARCHAR(20) NOT NULL COMMENT 'PENDIENTE, APROBADA, DENEGADA',
    tipo_operacion VARCHAR(20) NOT NULL COMMENT 'CREAR, REVOCAR',
    motivo_denegacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_procesamiento TIMESTAMP NULL,
    procesado_por VARCHAR(100),
    INDEX idx_peticion_id (peticion_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Tablas para Service Equipamiento
-- =============================================================================
USE onboarding_equip;

CREATE TABLE IF NOT EXISTS peticion_equipamiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    peticion_id VARCHAR(100) NOT NULL UNIQUE,
    workflow_id VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_email VARCHAR(100) NOT NULL,
    equipamiento_asignado JSON COMMENT 'Lista: portatil, monitor, VPN, etc.',
    estado VARCHAR(20) NOT NULL COMMENT 'PENDIENTE, APROBADA, DENEGADA',
    tipo_operacion VARCHAR(20) NOT NULL COMMENT 'ASIGNAR, REVOCAR',
    motivo_denegacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_procesamiento TIMESTAMP NULL,
    procesado_por VARCHAR(100),
    INDEX idx_peticion_id (peticion_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Mensaje de confirmación
-- =============================================================================
SELECT 'Todas las bases de datos y tablas han sido creadas correctamente' AS mensaje;
