package com.turnosodontologia.domain.financiero;

import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.comun.AuditableEntity;
import com.turnosodontologia.domain.identidad.Paciente;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuestos")
@Getter
public class Presupuesto extends AuditableEntity {

    public static final String ESTADO_EMITIDO = "emitido";
    public static final String ESTADO_ANULADO = "anulado";
    public static final String MONEDA = "ARS";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "presupuesto_seq")
    @SequenceGenerator(name = "presupuesto_seq", sequenceName = "presupuestos_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = ESTADO_EMITIDO;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = MONEDA;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO.setScale(2);

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPresupuesto> items = new ArrayList<>();

    protected Presupuesto() {
    }

    public Presupuesto(Paciente paciente, Turno turno) {
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente del presupuesto es obligatorio");
        }
        this.paciente = paciente;
        this.turno = turno;
    }

    public void asignarTurno(Turno turno) {
        verificarModificable();
        this.turno = turno;
    }

    public void agregarItem(Tratamiento tratamiento, int cantidad, BigDecimal precio, String descripcion) {
        verificarModificable();
        items.add(new ItemPresupuesto(this, tratamiento, cantidad, precio, descripcion));
        recalcularTotal();
    }

    public void limpiarItems() {
        verificarModificable();
        items.clear();
        recalcularTotal();
    }

    public void anular(String autor) {
        verificarModificable();
        this.estado = ESTADO_ANULADO;
        desactivar(autor);
    }

    public void recalcularTotal() {
        BigDecimal suma = items.stream()
                .map(ItemPresupuesto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = suma.setScale(2, RoundingMode.HALF_UP);
    }

    private void verificarModificable() {
        if (!ESTADO_EMITIDO.equals(this.estado)) {
            throw new PresupuestoNoModificableException("Un presupuesto anulado no puede modificarse");
        }
    }
}
