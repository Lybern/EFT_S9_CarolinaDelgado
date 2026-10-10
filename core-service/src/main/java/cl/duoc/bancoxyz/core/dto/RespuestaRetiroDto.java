package cl.duoc.bancoxyz.core.dto;

/**
 * DTO de respuesta para operaciones de retiro bancario.
 * Expone exclusivamente la informacion relevante para el cliente,
 * resguardando las propiedades y estructuras internas de la entidad Cuenta.
 */
public class RespuestaRetiroDto {

    private String transaccionId;
    private Long cuentaId;
    private Long montoRetirado;
    private Long nuevoSaldoContable;
    private Long saldoDisponibleTotal;
    private String canal;
    private String estado;
    private String mensaje;

    public RespuestaRetiroDto() {}

    public RespuestaRetiroDto(String transaccionId, Long cuentaId, Long montoRetirado,
                              Long nuevoSaldoContable, Long saldoDisponibleTotal,
                              String canal, String estado, String mensaje) {
        this.transaccionId = transaccionId;
        this.cuentaId = cuentaId;
        this.montoRetirado = montoRetirado;
        this.nuevoSaldoContable = nuevoSaldoContable;
        this.saldoDisponibleTotal = saldoDisponibleTotal;
        this.canal = canal;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transaccionId;
        private Long cuentaId;
        private Long montoRetirado;
        private Long nuevoSaldoContable;
        private Long saldoDisponibleTotal;
        private String canal;
        private String estado;
        private String mensaje;

        public Builder transaccionId(String transaccionId) { this.transaccionId = transaccionId; return this; }
        public Builder cuentaId(Long cuentaId) { this.cuentaId = cuentaId; return this; }
        public Builder montoRetirado(Long montoRetirado) { this.montoRetirado = montoRetirado; return this; }
        public Builder nuevoSaldoContable(Long nuevoSaldoContable) { this.nuevoSaldoContable = nuevoSaldoContable; return this; }
        public Builder saldoDisponibleTotal(Long saldoDisponibleTotal) { this.saldoDisponibleTotal = saldoDisponibleTotal; return this; }
        public Builder canal(String canal) { this.canal = canal; return this; }
        public Builder estado(String estado) { this.estado = estado; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }

        public RespuestaRetiroDto build() {
            return new RespuestaRetiroDto(transaccionId, cuentaId, montoRetirado, nuevoSaldoContable, saldoDisponibleTotal, canal, estado, mensaje);
        }
    }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public Long getMontoRetirado() { return montoRetirado; }
    public void setMontoRetirado(Long montoRetirado) { this.montoRetirado = montoRetirado; }

    public Long getNuevoSaldoContable() { return nuevoSaldoContable; }
    public void setNuevoSaldoContable(Long nuevoSaldoContable) { this.nuevoSaldoContable = nuevoSaldoContable; }

    public Long getSaldoDisponibleTotal() { return saldoDisponibleTotal; }
    public void setSaldoDisponibleTotal(Long saldoDisponibleTotal) { this.saldoDisponibleTotal = saldoDisponibleTotal; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
