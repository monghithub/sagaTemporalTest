package com.poc.onboarding.central.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para limpiar todas las bases de datos de los microservicios.
 * Uso exclusivo para desarrollo/testing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleanupService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Limpia todas las tablas de todos los microservicios.
     * @return Resumen de las tablas limpiadas
     */
    @Transactional
    public String limpiarTodasLasBases() {
        StringBuilder resultado = new StringBuilder();
        int totalEliminados = 0;

        // Limpiar App Central (primero paso_proceso por FK)
        int pasos = limpiarTabla("onboarding_central", "paso_proceso");
        int procesos = limpiarTabla("onboarding_central", "proceso_onboarding");
        resultado.append(String.format("Central: %d pasos, %d procesos eliminados. ", pasos, procesos));
        totalEliminados += pasos + procesos;

        // Limpiar Service LDAP
        int ldap = limpiarTabla("onboarding_ldap", "peticion_ldap");
        resultado.append(String.format("LDAP: %d. ", ldap));
        totalEliminados += ldap;

        // Limpiar Service Email
        int email = limpiarTabla("onboarding_email", "peticion_email");
        resultado.append(String.format("Email: %d. ", email));
        totalEliminados += email;

        // Limpiar Service Sistemas
        int sistemas = limpiarTabla("onboarding_sistemas", "peticion_sistemas");
        resultado.append(String.format("Sistemas: %d. ", sistemas));
        totalEliminados += sistemas;

        // Limpiar Service Equipamiento
        int equip = limpiarTabla("onboarding_equip", "peticion_equip");
        resultado.append(String.format("Equipamiento: %d. ", equip));
        totalEliminados += equip;

        log.info("Limpieza de BD completada. Total registros eliminados: {}", totalEliminados);

        return String.format("Limpieza completada. Total: %d registros eliminados. Detalle: %s",
                totalEliminados, resultado.toString());
    }

    private int limpiarTabla(String schema, String tabla) {
        try {
            String sql = String.format("DELETE FROM %s.%s", schema, tabla);
            int eliminados = jdbcTemplate.update(sql);
            log.info("Eliminados {} registros de {}.{}", eliminados, schema, tabla);
            return eliminados;
        } catch (Exception e) {
            log.warn("Error al limpiar {}.{}: {}", schema, tabla, e.getMessage());
            return 0;
        }
    }
}
