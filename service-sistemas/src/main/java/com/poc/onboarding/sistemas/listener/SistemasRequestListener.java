package com.poc.onboarding.sistemas.listener;

import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import com.poc.onboarding.common.event.PeticionCreatedEvent;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import com.poc.onboarding.sistemas.entity.PeticionSistemas;
import com.poc.onboarding.sistemas.service.PeticionService;
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
public class SistemasRequestListener {

    private final PeticionService peticionService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SISTEMAS_REQUESTS)
    public void handleRequest(PeticionCreatedEvent event) {
        log.info("Recibida petición SISTEMAS: {} - Operación: {}",
                event.getPeticionId(), event.getTipoOperacion());

        // Crear petición en BD
        PeticionSistemas peticion = PeticionSistemas.builder()
                .peticionId(event.getPeticionId())
                .workflowId(event.getWorkflowId())
                .empleadoNombre(event.getEmpleado().getNombre())
                .empleadoEmail(event.getEmpleado().getEmail())
                .estado(EstadoPeticion.PENDIENTE)
                .tipoOperacion(event.getTipoOperacion())
                .build();

        // Pre-generar username para mostrar en UI
        peticion.generarUsername();

        peticionService.guardar(peticion);

        log.info("Petición SISTEMAS {} guardada. Esperando aprobación manual.", event.getPeticionId());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SISTEMAS_COMPENSATE)
    public void handleCompensation(PeticionCreatedEvent event) {
        log.info("Recibida compensación SISTEMAS: {} - Usuario a eliminar: {}",
                event.getPeticionId(), event.getEmpleado().getUsernameGenerado());

        // Para compensaciones, auto-aprobar (simular eliminación)
        // En un sistema real, esto podría requerir aprobación manual también

        Map<String, Object> datos = new HashMap<>();
        datos.put("username_eliminado", event.getEmpleado().getUsernameGenerado());

        PeticionResponseEvent response = PeticionResponseEvent.aprobada(
                event.getPeticionId(),
                event.getWorkflowId(),
                TipoPaso.SISTEMAS,
                datos,
                "SISTEMA"
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_SISTEMAS_RESPONSE,
                response
        );

        log.info("Compensación SISTEMAS {} procesada automáticamente", event.getPeticionId());
    }
}
