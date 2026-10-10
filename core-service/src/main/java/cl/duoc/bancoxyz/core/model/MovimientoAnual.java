package cl.duoc.bancoxyz.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class MovimientoAnual {
    private Long cuentaId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private String transaccion;
    private Long monto;
    private String descripcion;

    public MovimientoAnual() {}

    public MovimientoAnual(Long cuentaId, LocalDate fecha, String transaccion, Long monto, String descripcion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getTransaccion() { return transaccion; }
    public void setTransaccion(String transaccion) { this.transaccion = transaccion; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return "MovimientoAnual{" +
                "cuentaId=" + cuentaId +
                ", fecha=" + fecha +
                ", transaccion='" + transaccion + '\'' +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}
