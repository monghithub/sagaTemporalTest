package com.poc.onboarding.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO con los datos del empleado para el proceso de onboarding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String email;
    private String departamento;
    private String puesto;

    // Datos generados durante el proceso
    private String usernameGenerado;      // Generado por LDAP
    private String emailCorporativo;       // Generado por Email
}
