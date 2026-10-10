package cl.duoc.bancoxyz.core.dto;

/**
 * DTO de respuesta para operaciones de transferencia electronica de fondos.
 * Confirma el procesamiento de la transaccion sin exponer los detalles completos
 * de las entidades internas involucradas.
 */
public class RespuestaTransferenciaDto {

    private String transaccionId;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private Long montoTransferido;
    private String estado;
    private String mensaje;

    public RespuestaTransferenciaDto() {}

    public RespuestaTransferenciaDto(String transaccionId, Long cuentaOrigenId, Long cuentaDestinoId,
                                    Long montoTransferido, String estado, String mensaje) {
        this.transaccionId = transaccionId;
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.montoTransferido = montoTransferido;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transaccionId;
        private Long cuentaOrigenId;
        private Long cuentaDestinoId;
        private Long montoTransferido;
        private String estado;
        private String mensaje;

        public Builder transaccionId(String transaccionId) { this.transaccionId = transaccionId; return this; }
        public Builder cuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; return this; }
        public Builder cuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; return this; }
        public Builder montoTransferido(Long montoTransferido) { this.montoTransferido = montoTransferido; return this; }
        public Builder estado(String estado) { this.estado = estado; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }

        public RespuestaTransferenciaDto build() {
            return new RespuestaTransferenciaDto(transaccionId, cuentaOrigenId, cuentaDestinoId, montoTransferido, estado, mensaje);
        }
    }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public void setCuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; }

    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public void setCuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; }

    public Long getMontoTransferido() { return montoTransferido; }
    public void setMontoTransferido(Long montoTransferido) { this.montoTransferido = montoTransferido; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
