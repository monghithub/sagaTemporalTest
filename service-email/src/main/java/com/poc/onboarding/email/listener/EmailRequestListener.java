package com.poc.onboarding.email.listener;

import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import com.poc.onboarding.common.event.PeticionCreatedEvent;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import com.poc.onboarding.email.entity.PeticionEmail;
import com.poc.onboarding.email.service.PeticionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRequestListener {

    private final PeticionService peticionService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_REQUESTS)
    public void handleRequest(PeticionCreatedEvent event) {
        log.info("Recibida petición EMAIL: {} - Operación: {}",
                event.getPeticionId(), event.getTipoOperacion());

        // Crear petición en BD
        PeticionEmail peticion = PeticionEmail.builder()
                .peticionId(event.getPeticionId())
                .workflowId(event.getWorkflowId())
                .empleadoNombre(event.getEmpleado().getNombre())
                .empleadoEmail(event.getEmpleado().getEmail())
                .estado(EstadoPeticion.PENDIENTE)
                .tipoOperacion(event.getTipoOperacion())
                .build();

        // Pre-generar email corporativo para mostrar en UI
        peticion.generarEmailCorporativo();

        peticionService.guardar(peticion);

        log.info("Petición EMAIL {} guardada. Esperando aprobación manual.", event.getPeticionId());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_COMPENSATE)
    public void handleCompensation(PeticionCreatedEvent event) {
        log.info("Recibida compensación EMAIL: {} - Email a eliminar: {}",
                event.getPeticionId(), event.getEmpleado().getEmailCorporativo());

        // Marcar la petición como compensada (ROLLBACK)
        peticionService.marcarComoCompensada(event.getWorkflowId());

        // Enviar respuesta de compensación completada
        Map<String, Object> datos = new HashMap<>();
        datos.put("email_eliminado", event.getEmpleado().getEmailCorporativo());

        PeticionResponseEvent response = PeticionResponseEvent.aprobada(
                event.getPeticionId(),
                event.getWorkflowId(),
                TipoPaso.EMAIL,
                datos,
                "SISTEMA"
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_EMAIL_RESPONSE,
                response
        );

        log.info("Compensación EMAIL {} completada - registro marcado como ROLLBACK", event.getPeticionId());
    }
}
