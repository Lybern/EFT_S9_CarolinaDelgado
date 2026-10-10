package cl.duoc.bancoxyz.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class Transaccion {
    private Long id;
    private Long cuentaId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private Long monto;
    private String tipo;
    private String descripcion;
    private String canalOrigen;

    public Transaccion() {}

    public Transaccion(Long id, Long cuentaId, LocalDate fecha, Long monto, String tipo, String descripcion, String canalOrigen) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.canalOrigen = canalOrigen;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCanalOrigen() { return canalOrigen; }
    public void setCanalOrigen(String canalOrigen) { this.canalOrigen = canalOrigen; }

    @Override
    public String toString() {
        return "Transaccion{" +
                "id=" + id +
                ", cuentaId=" + cuentaId +
                ", fecha=" + fecha +
                ", monto=" + monto +
                ", tipo='" + tipo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", canalOrigen='" + canalOrigen + '\'' +
                '}';
    }
}
