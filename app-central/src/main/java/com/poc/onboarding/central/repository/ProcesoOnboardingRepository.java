package com.poc.onboarding.central.repository;

import com.poc.onboarding.central.entity.ProcesoOnboarding;
import com.poc.onboarding.common.dto.EstadoProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcesoOnboardingRepository extends JpaRepository<ProcesoOnboarding, Long> {

    Optional<ProcesoOnboarding> findByWorkflowId(String workflowId);

    List<ProcesoOnboarding> findByEstadoOrderByCreatedAtDesc(EstadoProceso estado);

    List<ProcesoOnboarding> findAllByOrderByCreatedAtDesc();
}
