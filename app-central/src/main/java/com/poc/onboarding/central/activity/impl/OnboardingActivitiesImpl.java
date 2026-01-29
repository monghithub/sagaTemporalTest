package com.poc.onboarding.central.activity.impl;

import com.poc.onboarding.central.activity.OnboardingActivities;
import com.poc.onboarding.central.entity.PasoProceso;
import com.poc.onboarding.central.entity.ProcesoOnboarding;
import com.poc.onboarding.central.repository.PasoProcesoRepository;
import com.poc.onboarding.central.repository.ProcesoOnboardingRepository;
import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.*;
import com.poc.onboarding.common.event.PeticionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación de las Activities del workflow de onboarding.
 * Se encarga de publicar peticiones en RabbitMQ y actualizar el estado en BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingActivitiesImpl implements OnboardingActivities {

    private final RabbitTemplate rabbitTemplate;
    private final ProcesoOnboardingRepository procesoRepository;
    private final PasoProcesoRepository pasoRepository;

    // === LDAP ===
    @Override
    public String enviarPeticionLdap(String workflowId, EmpleadoDTO empleado) {
        return enviarPeticion(workflowId, empleado, TipoPaso.LDAP, TipoOperacion.CREAR,
                RabbitMQConfig.ROUTING_LDAP_REQUEST);
    }

    @Override
    public void compensarLdap(String workflowId, EmpleadoDTO empleado) {
        enviarCompensacion(workflowId, empleado, TipoPaso.LDAP,
                RabbitMQConfig.ROUTING_LDAP_COMPENSATE);
    }

    // === EMAIL ===
    @Override
    public String enviarPeticionEmail(String workflowId, EmpleadoDTO empleado) {
        return enviarPeticion(workflowId, empleado, TipoPaso.EMAIL, TipoOperacion.CREAR,
                RabbitMQConfig.ROUTING_EMAIL_REQUEST);
    }

    @Override
    public void compensarEmail(String workflowId, EmpleadoDTO empleado) {
        enviarCompensacion(workflowId, empleado, TipoPaso.EMAIL,
                RabbitMQConfig.ROUTING_EMAIL_COMPENSATE);
    }

    // === SISTEMAS ===
    @Override
    public String enviarPeticionSistemas(String workflowId, EmpleadoDTO empleado) {
        return enviarPeticion(workflowId, empleado, TipoPaso.SISTEMAS, TipoOperacion.CREAR,
                RabbitMQConfig.ROUTING_SISTEMAS_REQUEST);
    }

    @Override
    public void compensarSistemas(String workflowId, EmpleadoDTO empleado) {
        enviarCompensacion(workflowId, empleado, TipoPaso.SISTEMAS,
                RabbitMQConfig.ROUTING_SISTEMAS_COMPENSATE);
    }

    // === EQUIPAMIENTO ===
    @Override
    public String enviarPeticionEquipamiento(String workflowId, EmpleadoDTO empleado) {
        return enviarPeticion(workflowId, empleado, TipoPaso.EQUIPAMIENTO, TipoOperacion.ASIGNAR,
                RabbitMQConfig.ROUTING_EQUIP_REQUEST);
    }

    @Override
    public void compensarEquipamiento(String workflowId, EmpleadoDTO empleado) {
        enviarCompensacion(workflowId, empleado, TipoPaso.EQUIPAMIENTO,
                RabbitMQConfig.ROUTING_EQUIP_COMPENSATE);
    }

    // === Persistencia ===
    @Override
    @Transactional
    public void actualizarEstadoProceso(String workflowId, String estado, String pasoActual) {
        procesoRepository.findByWorkflowId(workflowId).ifPresent(proceso -> {
            proceso.setEstado(EstadoProceso.valueOf(estado));
            if (pasoActual != null) {
                proceso.setPasoActual(TipoPaso.valueOf(pasoActual));
            }
            procesoRepository.save(proceso);
            log.info("Proceso {} actualizado: estado={}, paso={}", workflowId, estado, pasoActual);
        });
    }

    @Override
    @Transactional
    public void registrarPasoIniciado(String workflowId, String tipoPaso, String peticionId) {
        procesoRepository.findByWorkflowId(workflowId).ifPresent(proceso -> {
            PasoProceso paso = PasoProceso.builder()
                    .proceso(proceso)
                    .tipoPaso(TipoPaso.valueOf(tipoPaso))
                    .estado(EstadoPeticion.PENDIENTE)
                    .peticionId(peticionId)
                    .build();
            paso.iniciar(peticionId);
            proceso.addPaso(paso);
            procesoRepository.save(proceso);
            log.info("Paso {} iniciado para proceso {}", tipoPaso, workflowId);
        });
    }

    @Override
    @Transactional
    public void registrarPasoCompletado(String workflowId, String tipoPaso, boolean aprobado, String mensaje) {
        procesoRepository.findByWorkflowId(workflowId).ifPresent(proceso -> {
            pasoRepository.findByProcesoIdAndTipoPaso(proceso.getId(), TipoPaso.valueOf(tipoPaso))
                    .ifPresent(paso -> {
                        if (aprobado) {
                            paso.aprobar(null);
                        } else {
                            paso.denegar(mensaje);
                        }
                        pasoRepository.save(paso);
                        log.info("Paso {} completado para proceso {}: aprobado={}", tipoPaso, workflowId, aprobado);
                    });
        });
    }

    // === Métodos privados ===
    private String enviarPeticion(String workflowId, EmpleadoDTO empleado, TipoPaso tipoPaso,
            TipoOperacion tipoOperacion, String routingKey) {
        String peticionId = UUID.randomUUID().toString();

        PeticionCreatedEvent event = PeticionCreatedEvent.of(
                peticionId, workflowId, tipoPaso, tipoOperacion, empleado);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);

        log.info("Petición {} enviada: tipo={}, workflow={}", peticionId, tipoPaso, workflowId);
        return peticionId;
    }

    private void enviarCompensacion(String workflowId, EmpleadoDTO empleado, TipoPaso tipoPaso,
            String routingKey) {
        String peticionId = UUID.randomUUID().toString();
        TipoOperacion tipoOperacion = tipoPaso == TipoPaso.EQUIPAMIENTO ?
                TipoOperacion.REVOCAR : TipoOperacion.ELIMINAR;

        PeticionCreatedEvent event = PeticionCreatedEvent.of(
                peticionId, workflowId, tipoPaso, tipoOperacion, empleado);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);

        log.info("Compensación {} enviada: tipo={}, workflow={}", peticionId, tipoPaso, workflowId);
    }
}
