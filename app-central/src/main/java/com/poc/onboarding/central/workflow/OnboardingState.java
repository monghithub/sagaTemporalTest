package com.poc.onboarding.central.workflow;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.EstadoProceso;
import com.poc.onboarding.common.dto.TipoPaso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Estado actual del proceso de onboarding (para queries)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingState implements Serializable {

    private static final long serialVersionUID = 1L;

    private EstadoProceso estadoProceso;
    private TipoPaso pasoActual;
    private Map<TipoPaso, EstadoPeticion> estadoPasos;
    private String usernameGenerado;
    private String emailCorporativo;
    private boolean rollbackSolicitado;
    private String mensajeError;
}
