package com.poc.onboarding.central.entity;

import com.poc.onboarding.common.dto.EstadoPeticion;
import com.poc.onboarding.common.dto.TipoPaso;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entidad que representa un paso individual del proceso de onboarding
 */
@Entity
@Table(name = "paso_proceso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasoProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    @ToString.Exclude
    private ProcesoOnboarding proceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_paso", nullable = false, length = 20)
    private TipoPaso tipoPaso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPeticion estado;

    @Column(name = "peticion_id", length = 100)
    private String peticionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_entrada", columnDefinition = "json")
    private Map<String, Object> datosEntrada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_salida", columnDefinition = "json")
    private Map<String, Object> datosSalida;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Métodos de utilidad
    public void iniciar(String peticionId) {
        this.peticionId = peticionId;
        this.estado = EstadoPeticion.PENDIENTE;
        this.fechaInicio = LocalDateTime.now();
    }

    public void aprobar(Map<String, Object> datosSalida) {
        this.estado = EstadoPeticion.APROBADA;
        this.datosSalida = datosSalida;
        this.fechaFin = LocalDateTime.now();
    }

    public void denegar(String motivo) {
        this.estado = EstadoPeticion.DENEGADA;
        this.mensajeError = motivo;
        this.fechaFin = LocalDateTime.now();
    }

    public void compensar() {
        this.estado = EstadoPeticion.COMPENSADA;
        this.fechaFin = LocalDateTime.now();
    }
}
