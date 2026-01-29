package com.poc.onboarding.common.event;

import com.poc.onboarding.common.dto.TipoPaso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento publicado cuando un servicio mock responde a una petición.
 * Se envía desde los servicios mock a App Central via RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionResponseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String peticionId;          // UUID de la petición original
    private String workflowId;          // ID del workflow de Temporal
    private TipoPaso tipoPaso;          // Servicio que responde
    private boolean aprobado;           // true = aprobado, false = denegado
    private String motivo;              // Motivo si fue denegado
    private Map<String, Object> datosAdicionales;  // Datos generados (username, etc.)
    private LocalDateTime timestamp;
    private String procesadoPor;        // Quién procesó la petición

    public static PeticionResponseEvent aprobada(String peticionId, String workflowId,
            TipoPaso tipoPaso, Map<String, Object> datosAdicionales, String procesadoPor) {
        return PeticionResponseEvent.builder()
                .peticionId(peticionId)
                .workflowId(workflowId)
                .tipoPaso(tipoPaso)
                .aprobado(true)
                .datosAdicionales(datosAdicionales)
                .procesadoPor(procesadoPor)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static PeticionResponseEvent denegada(String peticionId, String workflowId,
            TipoPaso tipoPaso, String motivo, String procesadoPor) {
        return PeticionResponseEvent.builder()
                .peticionId(peticionId)
                .workflowId(workflowId)
                .tipoPaso(tipoPaso)
                .aprobado(false)
                .motivo(motivo)
                .procesadoPor(procesadoPor)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
