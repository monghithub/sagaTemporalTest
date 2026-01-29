package com.poc.onboarding.ldap.controller;

import com.poc.onboarding.ldap.service.PeticionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class LdapMockController {

    private final PeticionService peticionService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pendientes", peticionService.obtenerPendientes());
        model.addAttribute("historial", peticionService.obtenerHistorial());
        model.addAttribute("serviceName", "LDAP / Active Directory");
        model.addAttribute("serviceIcon", "bi-person");
        return "peticiones";
    }

    @PostMapping("/peticiones/{id}/aprobar")
    public String aprobar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            peticionService.aprobar(id, "operador@empresa.com");
            redirectAttributes.addFlashAttribute("mensaje", "Petición aprobada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al aprobar: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/peticiones/{id}/denegar")
    public String denegar(@PathVariable Long id,
                          @RequestParam(required = false, defaultValue = "Denegado por el operador") String motivo,
                          RedirectAttributes redirectAttributes) {
        try {
            peticionService.denegar(id, motivo, "operador@empresa.com");
            redirectAttributes.addFlashAttribute("mensaje", "Petición denegada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al denegar: " + e.getMessage());
        }
        return "redirect:/";
    }
}
