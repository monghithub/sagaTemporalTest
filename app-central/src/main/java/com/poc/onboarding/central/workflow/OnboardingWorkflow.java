package com.poc.onboarding.central.workflow;

import com.poc.onboarding.common.dto.EmpleadoDTO;
import com.poc.onboarding.common.dto.TipoPaso;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Interface del Workflow de Onboarding.
 * Define el contrato para el proceso de onboarding orquestado por Temporal.
 */
@WorkflowInterface
public interface OnboardingWorkflow {

    /**
     * Método principal que ejecuta el proceso de onboarding completo.
     *
     * @param empleado Datos del empleado a incorporar
     * @return Resultado del proceso de onboarding
     */
    @WorkflowMethod
    OnboardingResult ejecutarOnboarding(EmpleadoDTO empleado);

    /**
     * Signal para indicar que un paso ha sido aprobado o denegado.
     * Enviado desde App Central cuando recibe respuesta de un servicio mock.
     *
     * @param tipoPaso El paso que fue procesado
     * @param resultado Resultado de la aprobación
     */
    @SignalMethod
    void aprobarPaso(TipoPaso tipoPaso, ApprovalResult resultado);

    /**
     * Signal para solicitar rollback manual del proceso.
     * Puede ser invocado en cualquier momento desde App Central.
     */
    @SignalMethod
    void solicitarRollback();

    /**
     * Signal para reintentar un paso fallido.
     *
     * @param tipoPaso El paso a reintentar
     */
    @SignalMethod
    void reintentarPaso(TipoPaso tipoPaso);

    /**
     * Query para obtener el estado actual del workflow.
     *
     * @return Estado actual del proceso
     */
    @QueryMethod
    OnboardingState obtenerEstado();
}
