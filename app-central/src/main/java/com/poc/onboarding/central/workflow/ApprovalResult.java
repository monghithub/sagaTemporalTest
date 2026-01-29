package com.poc.onboarding.central.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Resultado de la aprobación/denegación de un paso
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean aprobado;
    private String motivo;
    private Map<String, Object> datosAdicionales;

    public static ApprovalResult aprobado(Map<String, Object> datos) {
        return ApprovalResult.builder()
                .aprobado(true)
                .datosAdicionales(datos)
                .build();
    }

    public static ApprovalResult denegado(String motivo) {
        return ApprovalResult.builder()
                .aprobado(false)
                .motivo(motivo)
                .build();
    }
}
