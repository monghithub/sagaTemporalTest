package com.poc.onboarding.central.entity;

import com.poc.onboarding.common.dto.EstadoProceso;
import com.poc.onboarding.common.dto.TipoPaso;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un proceso de onboarding completo
 */
@Entity
@Table(name = "proceso_onboarding")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false, unique = true, length = 100)
    private String workflowId;

    @Column(name = "run_id", length = 100)
    private String runId;

    @Column(name = "empleado_nombre", nullable = false, length = 100)
    private String empleadoNombre;

    @Column(name = "empleado_email", nullable = false, length = 100)
    private String empleadoEmail;

    @Column(name = "empleado_departamento", length = 100)
    private String empleadoDepartamento;

    @Column(name = "empleado_puesto", length = 100)
    private String empleadoPuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProceso estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "paso_actual", length = 20)
    private TipoPaso pasoActual;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "proceso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<PasoProceso> pasos = new ArrayList<>();

    // Métodos de utilidad
    public void addPaso(PasoProceso paso) {
        pasos.add(paso);
        paso.setProceso(this);
    }

    public void iniciar() {
        this.estado = EstadoProceso.EN_PROGRESO;
        this.fechaInicio = LocalDateTime.now();
    }

    public void completar() {
        this.estado = EstadoProceso.COMPLETADO;
        this.fechaFin = LocalDateTime.now();
    }

    public void marcarRollback() {
        this.estado = EstadoProceso.ROLLBACK;
        this.fechaFin = LocalDateTime.now();
    }

    public void marcarError() {
        this.estado = EstadoProceso.ERROR;
        this.fechaFin = LocalDateTime.now();
    }
}
