package com.poc.onboarding.equip.entity;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoOperacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "peticion_equip")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "peticion_id", nullable = false, unique = true, length = 100)
    private String peticionId;

    @Column(name = "workflow_id", nullable = false, length = 100)
    private String workflowId;

    @Column(name = "empleado_nombre", nullable = false, length = 100)
    private String empleadoNombre;

    @Column(name = "empleado_email", nullable = false, length = 100)
    private String empleadoEmail;

    @Column(name = "username_generado", length = 50)
    private String usernameGenerado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPeticion estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", nullable = false, length = 20)
    private TipoOperacion tipoOperacion;

    @Column(name = "motivo_denegacion", columnDefinition = "TEXT")
    private String motivoDenegacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "procesado_por", length = 100)
    private String procesadoPor;

    @Builder.Default
    @Column(name = "compensada", nullable = false)
    private Boolean compensada = false;

    @Column(name = "fecha_compensacion")
    private LocalDateTime fechaCompensacion;

    // Generar username a partir del email
    public void generarUsername() {
        if (empleadoEmail != null && empleadoEmail.contains("@")) {
            String[] parts = empleadoEmail.split("@")[0].split("\\.");
            if (parts.length >= 2) {
                this.usernameGenerado = parts[0].charAt(0) + parts[1];
            } else {
                this.usernameGenerado = parts[0];
            }
            this.usernameGenerado = this.usernameGenerado.toLowerCase();
        }
    }

    public void aprobar(String procesadoPor) {
        this.estado = EstadoPeticion.APROBADA;
        this.fechaProcesamiento = LocalDateTime.now();
        this.procesadoPor = procesadoPor;
    }

    public void denegar(String motivo, String procesadoPor) {
        this.estado = EstadoPeticion.DENEGADA;
        this.motivoDenegacion = motivo;
        this.fechaProcesamiento = LocalDateTime.now();
        this.procesadoPor = procesadoPor;
    }

    public void marcarComoCompensada() {
        this.compensada = true;
        this.fechaCompensacion = LocalDateTime.now();
    }
}
