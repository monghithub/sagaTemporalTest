package com.poc.onboarding.central.service;

import com.poc.onboarding.central.config.TemporalConfig;
import com.poc.onboarding.central.entity.ProcesoOnboarding;
import com.poc.onboarding.central.repository.ProcesoOnboardingRepository;
import com.poc.onboarding.central.workflow.OnboardingState;
import com.poc.onboarding.central.workflow.OnboardingWorkflow;
import com.poc.onboarding.common.dto.EmpleadoDTO;
import com.poc.onboarding.common.dto.EstadoProceso;
import com.poc.onboarding.common.dto.TipoPaso;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal para gestionar procesos de onboarding
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final WorkflowClient workflowClient;
    private final ProcesoOnboardingRepository procesoRepository;

    /**
     * Inicia un nuevo proceso de onboarding
     */
    @Transactional
    public ProcesoOnboarding iniciarOnboarding(EmpleadoDTO empleado) {
        String workflowId = "onboarding-" + UUID.randomUUID().toString();

        // Crear registro en BD
        ProcesoOnboarding proceso = ProcesoOnboarding.builder()
                .workflowId(workflowId)
                .empleadoNombre(empleado.getNombre())
                .empleadoEmail(empleado.getEmail())
                .empleadoDepartamento(empleado.getDepartamento())
                .empleadoPuesto(empleado.getPuesto())
                .estado(EstadoProceso.INICIADO)
                .fechaInicio(LocalDateTime.now())
                .build();
        proceso = procesoRepository.save(proceso);

        // Iniciar workflow en Temporal
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TemporalConfig.TASK_QUEUE)
                .build();

        OnboardingWorkflow workflow = workflowClient.newWorkflowStub(OnboardingWorkflow.class, options);

        // Ejecutar workflow de forma asíncrona
        WorkflowClient.start(workflow::ejecutarOnboarding, empleado);

        log.info("Onboarding iniciado: workflowId={}, empleado={}", workflowId, empleado.getNombre());

        return proceso;
    }

    /**
     * Obtiene todos los procesos de onboarding
     */
    public List<ProcesoOnboarding> listarProcesos() {
        return procesoRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Obtiene un proceso por ID
     */
    public Optional<ProcesoOnboarding> obtenerProceso(Long id) {
        return procesoRepository.findById(id);
    }

    /**
     * Obtiene un proceso por workflowId
     */
    public Optional<ProcesoOnboarding> obtenerProcesoPorWorkflowId(String workflowId) {
        return procesoRepository.findByWorkflowId(workflowId);
    }

    /**
     * Consulta el estado actual del workflow en Temporal
     */
    public OnboardingState obtenerEstadoWorkflow(String workflowId) {
        try {
            OnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    OnboardingWorkflow.class, workflowId);
            return workflow.obtenerEstado();
        } catch (Exception e) {
            log.error("Error al obtener estado del workflow {}: {}", workflowId, e.getMessage());
            return null;
        }
    }

    /**
     * Solicita rollback manual de un proceso
     */
    public void solicitarRollback(String workflowId) {
        log.info("Solicitando rollback para workflow: {}", workflowId);
        OnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                OnboardingWorkflow.class, workflowId);
        workflow.solicitarRollback();
    }

    /**
     * Reintenta un paso fallido
     */
    public void reintentarPaso(String workflowId, TipoPaso tipoPaso) {
        log.info("Solicitando reintento de paso {} para workflow: {}", tipoPaso, workflowId);
        OnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                OnboardingWorkflow.class, workflowId);
        workflow.reintentarPaso(tipoPaso);
    }
}
