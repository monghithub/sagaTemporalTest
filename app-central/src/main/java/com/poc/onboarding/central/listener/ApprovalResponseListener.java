package com.poc.onboarding.central.listener;

import com.poc.onboarding.central.workflow.ApprovalResult;
import com.poc.onboarding.central.workflow.OnboardingWorkflow;
import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener que recibe las respuestas de los servicios mock via RabbitMQ
 * y envía signals a los workflows de Temporal correspondientes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalResponseListener {

    private final WorkflowClient workflowClient;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_LDAP_RESPONSES)
    public void handleLdapResponse(PeticionResponseEvent event) {
        procesarRespuesta(event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_RESPONSES)
    public void handleEmailResponse(PeticionResponseEvent event) {
        procesarRespuesta(event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SISTEMAS_RESPONSES)
    public void handleSistemasResponse(PeticionResponseEvent event) {
        procesarRespuesta(event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EQUIP_RESPONSES)
    public void handleEquipamientoResponse(PeticionResponseEvent event) {
        procesarRespuesta(event);
    }

    private void procesarRespuesta(PeticionResponseEvent event) {
        log.info("Respuesta recibida: peticion={}, workflow={}, tipo={}, aprobado={}",
                event.getPeticionId(), event.getWorkflowId(), event.getTipoPaso(), event.isAprobado());

        try {
            // Obtener stub del workflow
            OnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    OnboardingWorkflow.class, event.getWorkflowId());

            // Crear resultado de aprobación
            ApprovalResult result = event.isAprobado() ?
                    ApprovalResult.aprobado(event.getDatosAdicionales()) :
                    ApprovalResult.denegado(event.getMotivo());

            // Enviar signal al workflow
            workflow.aprobarPaso(event.getTipoPaso(), result);

            log.info("Signal enviado al workflow {} para paso {}", event.getWorkflowId(), event.getTipoPaso());

        } catch (Exception e) {
            log.error("Error al procesar respuesta para workflow {}: {}", event.getWorkflowId(), e.getMessage());
        }
    }
}
