package com.poc.onboarding.email.service;

import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import com.poc.onboarding.email.entity.PeticionEmail;
import com.poc.onboarding.email.repository.PeticionEmailRepository;
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

    private final PeticionEmailRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public List<PeticionEmail> obtenerPendientes() {
        return repository.findByEstadoOrderByFechaCreacionDesc(EstadoPeticion.PENDIENTE);
    }

    public List<PeticionEmail> obtenerHistorial() {
        return repository.findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion.PENDIENTE);
    }

    public Optional<PeticionEmail> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public void aprobar(Long id, String procesadoPor) {
        repository.findById(id).ifPresent(peticion -> {
            // Generar email corporativo si es operación de crear
            if (peticion.getEmailCorporativo() == null) {
                peticion.generarEmailCorporativo();
            }

            peticion.aprobar(procesadoPor);
            repository.save(peticion);

            // Enviar respuesta a App Central
            Map<String, Object> datos = new HashMap<>();
            datos.put("email", peticion.getEmailCorporativo());

            PeticionResponseEvent response = PeticionResponseEvent.aprobada(
                    peticion.getPeticionId(),
                    peticion.getWorkflowId(),
                    TipoPaso.EMAIL,
                    datos,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_EMAIL_RESPONSE,
                    response
            );

            log.info("Petición EMAIL {} aprobada. Email corporativo: {}",
                    peticion.getPeticionId(), peticion.getEmailCorporativo());
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
                    TipoPaso.EMAIL,
                    motivo,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_EMAIL_RESPONSE,
                    response
            );

            log.info("Petición EMAIL {} denegada. Motivo: {}", peticion.getPeticionId(), motivo);
        });
    }

    @Transactional
    public PeticionEmail guardar(PeticionEmail peticion) {
        return repository.save(peticion);
    }

    @Transactional
    public void eliminarPorWorkflowId(String workflowId) {
        repository.findByWorkflowId(workflowId).ifPresent(peticion -> {
            repository.delete(peticion);
            log.info("Petición EMAIL eliminada por compensación. WorkflowId: {}", workflowId);
        });
    }
}
