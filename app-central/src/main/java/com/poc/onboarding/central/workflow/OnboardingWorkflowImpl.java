package com.poc.onboarding.central.workflow;

import com.poc.onboarding.central.activity.OnboardingActivities;
import com.poc.onboarding.common.dto.EmpleadoDTO;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.EstadoProceso;
import com.poc.onboarding.common.dto.TipoPaso;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del Workflow de Onboarding usando Temporal.
 * Orquesta los 4 pasos del proceso con el patrón Saga para compensaciones.
 */
@Slf4j
public class OnboardingWorkflowImpl implements OnboardingWorkflow {

    // Estado del workflow
    private final Map<TipoPaso, ApprovalResult> approvalResults = new HashMap<>();
    private final Map<TipoPaso, EstadoPeticion> estadoPasos = new HashMap<>();
    private boolean rollbackSolicitado = false;
    private boolean retryRequested = false;
    private TipoPaso pasoActual;
    private String usernameGenerado;
    private String emailCorporativo;
    private String mensajeError;

    // Activities
    private final OnboardingActivities activities = Workflow.newActivityStub(
            OnboardingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build()
    );

    @Override
    public OnboardingResult ejecutarOnboarding(EmpleadoDTO empleado) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("Iniciando onboarding para: {} (workflow: {})", empleado.getNombre(), workflowId);

        // Configurar Saga para compensaciones
        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)  // Compensar en orden inverso
                .build());

        try {
            // Actualizar estado inicial
            activities.actualizarEstadoProceso(workflowId, EstadoProceso.EN_PROGRESO.name(), null);

            // === PASO 1: LDAP ===
            ejecutarPaso(saga, workflowId, empleado, TipoPaso.LDAP);
            if (rollbackSolicitado) {
                throw new RuntimeException("Rollback solicitado manualmente");
            }

            // Guardar username generado
            ApprovalResult ldapResult = approvalResults.get(TipoPaso.LDAP);
            if (ldapResult != null && ldapResult.getDatosAdicionales() != null) {
                usernameGenerado = (String) ldapResult.getDatosAdicionales().get("username");
                empleado.setUsernameGenerado(usernameGenerado);
            }

            // === PASO 2: EMAIL ===
            ejecutarPaso(saga, workflowId, empleado, TipoPaso.EMAIL);
            if (rollbackSolicitado) {
                throw new RuntimeException("Rollback solicitado manualmente");
            }

            // Guardar email corporativo
            ApprovalResult emailResult = approvalResults.get(TipoPaso.EMAIL);
            if (emailResult != null && emailResult.getDatosAdicionales() != null) {
                emailCorporativo = (String) emailResult.getDatosAdicionales().get("email");
                empleado.setEmailCorporativo(emailCorporativo);
            }

            // === PASO 3: SISTEMAS ===
            ejecutarPaso(saga, workflowId, empleado, TipoPaso.SISTEMAS);
            if (rollbackSolicitado) {
                throw new RuntimeException("Rollback solicitado manualmente");
            }

            // === PASO 4: EQUIPAMIENTO ===
            ejecutarPaso(saga, workflowId, empleado, TipoPaso.EQUIPAMIENTO);
            if (rollbackSolicitado) {
                throw new RuntimeException("Rollback solicitado manualmente");
            }

            // Proceso completado exitosamente
            activities.actualizarEstadoProceso(workflowId, EstadoProceso.COMPLETADO.name(), null);
            log.info("Onboarding completado exitosamente para: {}", empleado.getNombre());

            return OnboardingResult.completado(usernameGenerado, emailCorporativo);

        } catch (Exception e) {
            log.error("Error en onboarding, ejecutando compensaciones: {}", e.getMessage());
            mensajeError = e.getMessage();

            // Ejecutar compensaciones
            saga.compensate();

            activities.actualizarEstadoProceso(workflowId, EstadoProceso.ROLLBACK.name(), null);
            return OnboardingResult.rollback(e.getMessage());
        }
    }

    private void ejecutarPaso(Saga saga, String workflowId, EmpleadoDTO empleado, TipoPaso tipoPaso) {
        pasoActual = tipoPaso;
        estadoPasos.put(tipoPaso, EstadoPeticion.PENDIENTE);

        // Actualizar estado en BD
        activities.actualizarEstadoProceso(workflowId, EstadoProceso.EN_PROGRESO.name(), tipoPaso.name());

        // Registrar compensación antes de ejecutar el paso
        registrarCompensacion(saga, workflowId, empleado, tipoPaso);

        // Enviar petición al servicio correspondiente
        String peticionId = enviarPeticion(workflowId, empleado, tipoPaso);

        // Registrar paso iniciado
        activities.registrarPasoIniciado(workflowId, tipoPaso.name(), peticionId);

        // Esperar aprobación (signal)
        log.info("Esperando aprobación para paso: {}", tipoPaso);
        Workflow.await(() -> approvalResults.containsKey(tipoPaso) || rollbackSolicitado);

        if (rollbackSolicitado) {
            estadoPasos.put(tipoPaso, EstadoPeticion.DENEGADA);
            throw new RuntimeException("Rollback solicitado durante " + tipoPaso);
        }

        ApprovalResult result = approvalResults.get(tipoPaso);
        if (!result.isAprobado()) {
            estadoPasos.put(tipoPaso, EstadoPeticion.DENEGADA);
            activities.registrarPasoCompletado(workflowId, tipoPaso.name(), false, result.getMotivo());
            throw new RuntimeException("Paso " + tipoPaso + " denegado: " + result.getMotivo());
        }

        estadoPasos.put(tipoPaso, EstadoPeticion.APROBADA);
        activities.registrarPasoCompletado(workflowId, tipoPaso.name(), true, null);
        log.info("Paso {} completado exitosamente", tipoPaso);
    }

    private String enviarPeticion(String workflowId, EmpleadoDTO empleado, TipoPaso tipoPaso) {
        return switch (tipoPaso) {
            case LDAP -> activities.enviarPeticionLdap(workflowId, empleado);
            case EMAIL -> activities.enviarPeticionEmail(workflowId, empleado);
            case SISTEMAS -> activities.enviarPeticionSistemas(workflowId, empleado);
            case EQUIPAMIENTO -> activities.enviarPeticionEquipamiento(workflowId, empleado);
        };
    }

    private void registrarCompensacion(Saga saga, String workflowId, EmpleadoDTO empleado, TipoPaso tipoPaso) {
        saga.addCompensation(() -> {
            log.info("Ejecutando compensación para: {}", tipoPaso);
            switch (tipoPaso) {
                case LDAP -> activities.compensarLdap(workflowId, empleado);
                case EMAIL -> activities.compensarEmail(workflowId, empleado);
                case SISTEMAS -> activities.compensarSistemas(workflowId, empleado);
                case EQUIPAMIENTO -> activities.compensarEquipamiento(workflowId, empleado);
            }
        });
    }

    @Override
    public void aprobarPaso(TipoPaso tipoPaso, ApprovalResult resultado) {
        log.info("Recibida aprobación para paso {}: aprobado={}", tipoPaso, resultado.isAprobado());
        approvalResults.put(tipoPaso, resultado);
    }

    @Override
    public void solicitarRollback() {
        log.info("Rollback manual solicitado");
        rollbackSolicitado = true;
    }

    @Override
    public void reintentarPaso(TipoPaso tipoPaso) {
        log.info("Reintento solicitado para paso: {}", tipoPaso);
        approvalResults.remove(tipoPaso);
        retryRequested = true;
    }

    @Override
    public OnboardingState obtenerEstado() {
        return OnboardingState.builder()
                .estadoProceso(rollbackSolicitado ? EstadoProceso.ROLLBACK : EstadoProceso.EN_PROGRESO)
                .pasoActual(pasoActual)
                .estadoPasos(new HashMap<>(estadoPasos))
                .usernameGenerado(usernameGenerado)
                .emailCorporativo(emailCorporativo)
                .rollbackSolicitado(rollbackSolicitado)
                .mensajeError(mensajeError)
                .build();
    }
}
