package com.poc.onboarding.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para representar una petición enviada a los servicios mock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String peticionId;          // UUID único de la petición
    private String workflowId;          // ID del workflow de Temporal
    private TipoPaso tipoPaso;          // LDAP, EMAIL, SISTEMAS, EQUIPAMIENTO
    private TipoOperacion tipoOperacion; // CREAR, ELIMINAR, etc.
    private EmpleadoDTO empleado;       // Datos del empleado
    private EstadoPeticion estado;      // Estado actual
    private String motivo;              // Motivo de denegación si aplica
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaProcesamiento;
}
