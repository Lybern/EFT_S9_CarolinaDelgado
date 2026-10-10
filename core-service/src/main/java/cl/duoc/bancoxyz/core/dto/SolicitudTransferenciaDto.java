package cl.duoc.bancoxyz.core.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de solicitud para transferencias electronicas de fondos entre cuentas.
 * Valida que los datos obligatorios de origen, destino y monto cumplan con
 * las reglas minimas del negocio antes de pasar a la capa de servicio.
 */
public class SolicitudTransferenciaDto {

    @NotNull(message = "El identificador de la cuenta origen es obligatorio")
    private Long cuentaOrigenId;

    @NotNull(message = "El identificador de la cuenta destino es obligatorio")
    private Long cuentaDestinoId;

    @NotNull(message = "El monto a transferir es obligatorio")
    @Min(value = 100, message = "El monto minimo para transferencias es de $100")
    private Long monto;

    private String comentario;

    public SolicitudTransferenciaDto() {}

    public SolicitudTransferenciaDto(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String comentario) {
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.comentario = comentario;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long cuentaOrigenId;
        private Long cuentaDestinoId;
        private Long monto;
        private String comentario;

        public Builder cuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; return this; }
        public Builder cuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; return this; }
        public Builder monto(Long monto) { this.monto = monto; return this; }
        public Builder comentario(String comentario) { this.comentario = comentario; return this; }

        public SolicitudTransferenciaDto build() {
            return new SolicitudTransferenciaDto(cuentaOrigenId, cuentaDestinoId, monto, comentario);
        }
    }

    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public void setCuentaOrigenId(Long cuentaOrigenId) { this.cuentaOrigenId = cuentaOrigenId; }

    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public void setCuentaDestinoId(Long cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
