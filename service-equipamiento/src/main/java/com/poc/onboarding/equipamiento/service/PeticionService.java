package com.poc.onboarding.equip.service;

import com.poc.onboarding.common.config.RabbitMQConfig;
import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import com.poc.onboarding.common.event.PeticionResponseEvent;
import com.poc.onboarding.equip.entity.PeticionEquipamiento;
import com.poc.onboarding.equip.repository.PeticionEquipamientoRepository;
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

    private final PeticionEquipamientoRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public List<PeticionEquipamiento> obtenerPendientes() {
        return repository.findByEstadoOrderByFechaCreacionDesc(EstadoPeticion.PENDIENTE);
    }

    public List<PeticionEquipamiento> obtenerHistorial() {
        return repository.findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion.PENDIENTE);
    }

    public Optional<PeticionEquipamiento> obtenerPorId(Long id) {
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
                    TipoPaso.EQUIPAMIENTO,
                    datos,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_EQUIP_RESPONSE,
                    response
            );

            log.info("Petición EQUIPAMIENTO {} aprobada. Username generado: {}",
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
                    TipoPaso.EQUIPAMIENTO,
                    motivo,
                    procesadoPor
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_EQUIP_RESPONSE,
                    response
            );

            log.info("Petición EQUIPAMIENTO {} denegada. Motivo: {}", peticion.getPeticionId(), motivo);
        });
    }

    @Transactional
    public PeticionEquipamiento guardar(PeticionEquipamiento peticion) {
        return repository.save(peticion);
    }

    @Transactional
    public void eliminarPorWorkflowId(String workflowId) {
        repository.findByWorkflowId(workflowId).ifPresent(peticion -> {
            repository.delete(peticion);
            log.info("Petición EQUIPAMIENTO eliminada por compensación. WorkflowId: {}", workflowId);
        });
    }
}
