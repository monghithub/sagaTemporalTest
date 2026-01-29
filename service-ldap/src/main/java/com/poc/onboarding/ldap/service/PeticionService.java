package com.poc.onboarding.ldap.service;

import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import com.poc.onboarding.ldap.entity.PeticionLdap;
import com.poc.onboarding.ldap.repository.PeticionLdapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeticionService {

    private final PeticionLdapRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public List<PeticionLdap> obtenerPendientes() {
        return repository.findByEstadoOrderByFechaCreacionDesc(EstadoPeticion.PENDIENTE);
    }

    public List<PeticionLdap> obtenerHistorial() {
        return repository.findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion.PENDIENTE);
    }

    public Optional<PeticionLdap> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public void aprobar(Long id, String procesadoPor) {
        repository.findById(id).ifPresent(peticion -> {
            // Generar username si es operación de crear
            if (peticion.getUsernameGenerado() == null) {
                peticion.generarUsername();
            }

            peticion.aprobar(procesadoPor);
            repository.save(peticion);

            // Enviar respuesta a App Central
            Map<String, Object> datos = new HashMap<>();
            datos.put("username", peticion.getUsernameGenerado());

            PeticionResponseEvent response = PeticionResponseEvent.aprobada(
                    peticion.getPeticionId(),
                    peticion.getWorkflowId(),
                    TipoPaso.LDAP,
                    datos,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_LDAP_RESPONSE,
                    response
            );

            log.info("Petición LDAP {} aprobada. Username generado: {}",
                    peticion.getPeticionId(), peticion.getUsernameGenerado());
        });
    }

    @Transactional
    public void denegar(Long id, String motivo, String procesadoPor) {
        repository.findById(id).ifPresent(peticion -> {
            peticion.denegar(motivo, procesadoPor);
            repository.save(peticion);

            // Enviar respuesta a App Central
            PeticionResponseEvent response = PeticionResponseEvent.denegada(
                    peticion.getPeticionId(),
                    peticion.getWorkflowId(),
                    TipoPaso.LDAP,
                    motivo,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_LDAP_RESPONSE,
                    response
            );

            log.info("Petición LDAP {} denegada. Motivo: {}", peticion.getPeticionId(), motivo);
        });
    }

    @Transactional
    public PeticionLdap guardar(PeticionLdap peticion) {
        return repository.save(peticion);
    }

    @Transactional
    public void eliminarPorWorkflowId(String workflowId) {
        repository.findByWorkflowId(workflowId).ifPresent(peticion -> {
            repository.delete(peticion);
            log.info("Petición LDAP eliminada por compensación. WorkflowId: {}", workflowId);
        });
    }
}
