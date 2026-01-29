package com.poc.onboarding.central.workflow;

import com.poc.onboarding.common.dto.EstadoProceso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Resultado final del proceso de onboarding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private EstadoProceso estado;
    private String mensaje;
    private String usernameGenerado;
    private String emailCorporativo;
    private Map<String, Object> datosAdicionales;

    public static OnboardingResult completado(String username, String email) {
        return OnboardingResult.builder()
                .estado(EstadoProceso.COMPLETADO)
                .mensaje("Onboarding completado exitosamente")
                .usernameGenerado(username)
                .emailCorporativo(email)
                .build();
    }

    public static OnboardingResult rollback(String motivo) {
        return OnboardingResult.builder()
                .estado(EstadoProceso.ROLLBACK)
                .mensaje("Onboarding cancelado: " + motivo)
                .build();
    }

    public static OnboardingResult error(String error) {
        return OnboardingResult.builder()
                .estado(EstadoProceso.ERROR)
                .mensaje("Error en onboarding: " + error)
                .build();
    }
}
