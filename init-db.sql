-- =============================================================================
-- Script de inicialización de bases de datos
-- Ejecutado automáticamente por MySQL al arrancar el contenedor
-- Las tablas serán creadas por Hibernate con ddl-auto: update
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

-- Mensaje de confirmación
SELECT 'Todas las bases de datos han sido creadas correctamente' AS mensaje;
