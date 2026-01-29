package com.poc.onboarding.central.config;

import com.poc.onboarding.central.activity.impl.OnboardingActivitiesImpl;
import com.poc.onboarding.central.workflow.OnboardingWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración que inicializa el Worker de Temporal al arrancar la aplicación
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalWorkerConfig {

    private final WorkerFactory workerFactory;
    private final OnboardingActivitiesImpl onboardingActivities;

    @PostConstruct
    public void startWorker() {
        log.info("Iniciando Temporal Worker para task queue: {}", TemporalConfig.TASK_QUEUE);

        // Crear worker
        Worker worker = workerFactory.newWorker(TemporalConfig.TASK_QUEUE);

        // Registrar workflow
        worker.registerWorkflowImplementationTypes(OnboardingWorkflowImpl.class);

        // Registrar activities
        worker.registerActivitiesImplementations(onboardingActivities);

        // Iniciar factory
        workerFactory.start();

        log.info("Temporal Worker iniciado correctamente");
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Deteniendo Temporal Worker...");
        workerFactory.shutdown();
    }
}
