package com.poc.onboarding.central.repository;

import com.poc.onboarding.central.entity.PasoProceso;
import com.poc.onboarding.common.dto.TipoPaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasoProcesoRepository extends JpaRepository<PasoProceso, Long> {

    Optional<PasoProceso> findByPeticionId(String peticionId);

    Optional<PasoProceso> findByProcesoIdAndTipoPaso(Long procesoId, TipoPaso tipoPaso);
}
