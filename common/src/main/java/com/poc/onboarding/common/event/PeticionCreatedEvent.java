package com.poc.onboarding.common.event;

import com.poc.onboarding.common.dto.EmpleadoDTO;
import com.poc.onboarding.common.dto.TipoOperacion;
import com.poc.onboarding.common.dto.TipoPaso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando se crea una nueva petición.
 * Se envía desde App Central a los servicios mock via RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String peticionId;          // UUID único
    private String workflowId;          // ID del workflow de Temporal
    private TipoPaso tipoPaso;          // Servicio destino
    private TipoOperacion tipoOperacion; // CREAR o compensación
    private EmpleadoDTO empleado;       // Datos del empleado
    private LocalDateTime timestamp;

    public static PeticionCreatedEvent of(String peticionId, String workflowId,
            TipoPaso tipoPaso, TipoOperacion tipoOperacion, EmpleadoDTO empleado) {
        return PeticionCreatedEvent.builder()
                .peticionId(peticionId)
                .workflowId(workflowId)
                .tipoPaso(tipoPaso)
                .tipoOperacion(tipoOperacion)
                .empleado(empleado)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
