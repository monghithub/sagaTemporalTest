package com.poc.onboarding.central.activity;

import com.poc.onboarding.common.dto.EmpleadoDTO;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Interface de Activities para el proceso de onboarding.
 * Cada método publica una petición en RabbitMQ al servicio correspondiente.
 */
@ActivityInterface
public interface OnboardingActivities {

    // === LDAP ===
    @ActivityMethod
    String enviarPeticionLdap(String workflowId, EmpleadoDTO empleado);

    @ActivityMethod
    void compensarLdap(String workflowId, EmpleadoDTO empleado);

    // === EMAIL ===
    @ActivityMethod
    String enviarPeticionEmail(String workflowId, EmpleadoDTO empleado);

    @ActivityMethod
    void compensarEmail(String workflowId, EmpleadoDTO empleado);

    // === SISTEMAS ===
    @ActivityMethod
    String enviarPeticionSistemas(String workflowId, EmpleadoDTO empleado);

    @ActivityMethod
    void compensarSistemas(String workflowId, EmpleadoDTO empleado);

    // === EQUIPAMIENTO ===
    @ActivityMethod
    String enviarPeticionEquipamiento(String workflowId, EmpleadoDTO empleado);

    @ActivityMethod
    void compensarEquipamiento(String workflowId, EmpleadoDTO empleado);

    // === Persistencia ===
    @ActivityMethod
    void actualizarEstadoProceso(String workflowId, String estado, String pasoActual);

    @ActivityMethod
    void registrarPasoIniciado(String workflowId, String tipoPaso, String peticionId);

    @ActivityMethod
    void registrarPasoCompletado(String workflowId, String tipoPaso, boolean aprobado, String mensaje);
}
