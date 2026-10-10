package cl.duoc.bancoxyz.mensajeria.mensajeria;

import java.io.Serializable;

public class EventoTransaccion implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transaccionId;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private Long monto;
    private String tipoOperacion; // RETIRO, TRANSFERENCIA
    private String canal;         // MOVIL, WEB, ATM
    private String estado;        // EXITOSA, CONTINGENCIA
    private String fechaHora;
    private String detalle;

    public EventoTransaccion() {}

    public EventoTransaccion(String transaccionId, Long cuentaOrigenId, Long cuentaDestinoId,
                             Long monto, String tipoOperacion, String canal,
                             String estado, String fechaHora, String detalle) {
        this.transaccionId = transaccionId;
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.tipoOperacion = tipoOperacion;
        this.canal = canal;
        this.estado = estado;
        this.fechaHora = fechaHora;
        this.detalle = detalle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transaccionId;
        private Long cuentaOrigenId;
        private Long cuentaDestinoId;
        private Long monto;
        private String tipoOperacion;
        private String canal;
        private String estado;
        private String fechaHora;
        private String detalle;

        public Builder transaccionId(String transaccionId) { this.transaccionId = transaccionId; return this; }
        public Builder cuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; return this; }
        public Builder cuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; return this; }
        public Builder monto(Long monto) { this.monto = monto; return this; }
        public Builder tipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; return this; }
        public Builder canal(String canal) { this.canal = canal; return this; }
        public Builder estado(String estado) { this.estado = estado; return this; }
        public Builder fechaHora(String fechaHora) { this.fechaHora = fechaHora; return this; }
        public Builder detalle(String detalle) { this.detalle = detalle; return this; }

        public EventoTransaccion build() {
            return new EventoTransaccion(transaccionId, cuentaOrigenId, cuentaDestinoId, monto, tipoOperacion, canal, estado, fechaHora, detalle);
        }
    }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public void setCuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; }

    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public void setCuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    @Override
    public String toString() {
        return "EventoTransaccion{" +
                "transaccionId='" + transaccionId + '\'' +
                ", cuentaOrigenId=" + cuentaOrigenId +
                ", cuentaDestinoId=" + cuentaDestinoId +
                ", monto=" + monto +
                ", tipoOperacion='" + tipoOperacion + '\'' +
                ", canal='" + canal + '\'' +
                ", estado='" + estado + '\'' +
                ", fechaHora='" + fechaHora + '\'' +
                ", detalle='" + detalle + '\'' +
                '}';
    }
}
