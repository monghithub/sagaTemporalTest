package com.poc.onboarding.ldap.repository;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.ldap.entity.PeticionLdap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeticionLdapRepository extends JpaRepository<PeticionLdap, Long> {

    Optional<PeticionLdap> findByPeticionId(String peticionId);

    List<PeticionLdap> findByEstadoOrderByFechaCreacionDesc(EstadoPeticion estado);

    List<PeticionLdap> findByEstadoNotOrderByFechaProcesamiento(EstadoPeticion estado);

    List<PeticionLdap> findAllByOrderByFechaCreacionDesc();

    Optional<PeticionLdap> findByWorkflowId(String workflowId);

    void deleteByWorkflowId(String workflowId);
}
