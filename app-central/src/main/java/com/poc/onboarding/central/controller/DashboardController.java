package com.poc.onboarding.central.controller;

import com.poc.onboarding.central.entity.ProcesoOnboarding;
import com.poc.onboarding.central.service.OnboardingService;
import com.poc.onboarding.central.workflow.OnboardingState;
import com.poc.onboarding.common.dto.EmpleadoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller para las vistas del dashboard (Thymeleaf)
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final OnboardingService onboardingService;

    /**
     * Página principal - Dashboard con lista de procesos
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("procesos", onboardingService.listarProcesos());
        model.addAttribute("nuevoEmpleado", new EmpleadoDTO());
        return "dashboard";
    }

    /**
     * Detalle de un proceso
     */
    @GetMapping("/proceso/{id}")
    public String detalleProceso(@PathVariable Long id, Model model) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    model.addAttribute("proceso", proceso);

                    // Obtener estado del workflow si está activo
                    OnboardingState state = onboardingService.obtenerEstadoWorkflow(proceso.getWorkflowId());
                    model.addAttribute("workflowState", state);

                    return "proceso-detalle";
                })
                .orElse("redirect:/");
    }

    /**
     * Crear nuevo proceso de onboarding
     */
    @PostMapping("/nuevo-proceso")
    public String crearProceso(@ModelAttribute EmpleadoDTO empleado, RedirectAttributes redirectAttributes) {
        try {
            ProcesoOnboarding proceso = onboardingService.iniciarOnboarding(empleado);
            redirectAttributes.addFlashAttribute("mensaje", "Proceso de onboarding iniciado correctamente");
            return "redirect:/proceso/" + proceso.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al iniciar proceso: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Solicitar rollback desde el dashboard
     */
    @PostMapping("/proceso/{id}/rollback")
    public String rollback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    try {
                        onboardingService.solicitarRollback(proceso.getWorkflowId());
                        redirectAttributes.addFlashAttribute("mensaje", "Rollback solicitado correctamente");
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("error", "Error al solicitar rollback: " + e.getMessage());
                    }
                    return "redirect:/proceso/" + id;
                })
                .orElse("redirect:/");
    }

    /**
     * Reintentar paso desde el dashboard
     */
    @PostMapping("/proceso/{id}/retry/{paso}")
    public String reintentarPaso(@PathVariable Long id, @PathVariable String paso, RedirectAttributes redirectAttributes) {
        return onboardingService.obtenerProceso(id)
                .map(proceso -> {
                    try {
                        onboardingService.reintentarPaso(proceso.getWorkflowId(),
                                com.poc.onboarding.common.dto.TipoPaso.valueOf(paso.toUpperCase()));
                        redirectAttributes.addFlashAttribute("mensaje", "Reintento solicitado para paso: " + paso);
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("error", "Error al reintentar: " + e.getMessage());
                    }
                    return "redirect:/proceso/" + id;
                })
                .orElse("redirect:/");
    }
}
