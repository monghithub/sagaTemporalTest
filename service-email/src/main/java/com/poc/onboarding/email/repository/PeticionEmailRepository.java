package com.poc.onboarding.email.repository;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.email.entity.PeticionEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeticionEmailRepository extends JpaRepository<PeticionEmail, Long> {

    Optional<PeticionEmail> findByPeticionId(String peticionId);

    List<PeticionEmail> findByEstadoOrderByFechaCreacionDesc(EstadoPeticion estado);

    List<PeticionEmail> findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion estado);

    List<PeticionEmail> findAllByOrderByFechaCreacionDesc();
}
