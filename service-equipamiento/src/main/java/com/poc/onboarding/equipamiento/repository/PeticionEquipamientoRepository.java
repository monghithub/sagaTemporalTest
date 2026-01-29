package com.poc.onboarding.equip.repository;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.equip.entity.PeticionEquipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeticionEquipamientoRepository extends JpaRepository<PeticionEquipamiento, Long> {

    Optional<PeticionEquipamiento> findByPeticionId(String peticionId);

    List<PeticionEquipamiento> findByEstadoOrderByFechaCreacionDesc(EstadoPeticion estado);

    List<PeticionEquipamiento> findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion estado);

    List<PeticionEquipamiento> findAllByOrderByFechaCreacionDesc();
}
