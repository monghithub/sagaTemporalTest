package com.poc.onboarding.sistemas.repository;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.sistemas.entity.PeticionSistemas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeticionSistemasRepository extends JpaRepository<PeticionSistemas, Long> {

    Optional<PeticionSistemas> findByPeticionId(String peticionId);

    List<PeticionSistemas> findByEstadoOrderByFechaCreacionDesc(EstadoPeticion estado);

    List<PeticionSistemas> findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion estado);

    List<PeticionSistemas> findAllByOrderByFechaCreacionDesc();

    Optional<PeticionSistemas> findByWorkflowId(String workflowId);

    void deleteByWorkflowId(String workflowId);
}
