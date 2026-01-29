package com.poc.onboarding.central.controller;

import com.poc.onboarding.central.entity.ProcesoOnboarding;
import com.poc.onboarding.central.service.OnboardingService;
import com.poc.onboarding.central.workflow.OnboardingState;
import com.poc.onboarding.common.dto.EmpleadoDTO;
import com.poc.onboarding.common.dto.TipoPaso;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST para gestión de procesos de onboarding
 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Inicia un nuevo proceso de onboarding
     */
    @PostMapping
    public ResponseEntity<ProcesoOnboarding> iniciarOnboarding(@RequestBody EmpleadoDTO empleado) {
        ProcesoOnboarding proceso = onboardingService.iniciarOnboarding(empleado);
        return ResponseEntity.ok(proceso);
    }

    /**
     * Lista todos los procesos de onboarding
     */
    @GetMapping
    public ResponseEntity<List<ProcesoOnboarding>> listarProcesos() {
        return ResponseEntity.ok(onboardingService.listarProcesos());
    }

    /**
     * Obtiene un proceso por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcesoOnboarding> obtenerProceso(@PathVariable Long id) {
        return onboardingService.obtenerProceso(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene el estado actual del workflow
     */
    @GetMapping("/{id}/estado")
    public ResponseEntity<OnboardingState> obtenerEstado(@PathVariable Long id) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    OnboardingState state = onboardingService.obtenerEstadoWorkflow(proceso.getWorkflowId());
                    return state != null ? ResponseEntity.ok(state) : ResponseEntity.notFound().<OnboardingState>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Solicita rollback de un proceso
     */
    @PostMapping("/{id}/rollback")
    public ResponseEntity<Void> solicitarRollback(@PathVariable Long id) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    onboardingService.solicitarRollback(proceso.getWorkflowId());
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reintenta un paso fallido
     */
    @PostMapping("/{id}/retry/{paso}")
    public ResponseEntity<Void> reintentarPaso(@PathVariable Long id, @PathVariable String paso) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    onboardingService.reintentarPaso(proceso.getWorkflowId(), TipoPaso.valueOf(paso.toUpperCase()));
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
