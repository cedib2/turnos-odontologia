package com.turnosodontologia.domain.financiero;

import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "items_presupuesto")
@Getter
public class ItemPresupuesto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_presupuesto_seq")
    @SequenceGenerator(name = "item_presupuesto_seq", sequenceName = "items_presupuesto_id_seq",
            allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private Presupuesto presupuesto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    protected ItemPresupuesto() {
    }

    public ItemPresupuesto(Presupuesto presupuesto, Tratamiento tratamiento, int cantidad,
                           BigDecimal precio, String descripcion) {
        validar(tratamiento, cantidad, precio, descripcion);
        if (presupuesto == null) {
            throw new IllegalArgumentException("El presupuesto del ítem es obligatorio");
        }
        this.presupuesto = presupuesto;
        this.tratamiento = tratamiento;
        this.cantidad = cantidad;
        this.precio = precio.setScale(2, RoundingMode.HALF_UP);
        this.descripcion = descripcion.trim();
    }

    public void actualizar(Tratamiento tratamiento, int cantidad, BigDecimal precio, String descripcion) {
        validar(tratamiento, cantidad, precio, descripcion);
        this.tratamiento = tratamiento;
        this.cantidad = cantidad;
        this.precio = precio.setScale(2, RoundingMode.HALF_UP);
        this.descripcion = descripcion.trim();
    }

    public BigDecimal subtotal() {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }

    private void validar(Tratamiento tratamiento, int cantidad, BigDecimal precio, String descripcion) {
        if (tratamiento == null) {
            throw new IllegalArgumentException("El tratamiento del ítem es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del ítem debe ser positiva");
        }
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio del ítem no puede ser negativo");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del ítem no puede estar vacía");
        }
    }
}
