package cl.duoc.bancoxyz.core.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de solicitud para la ejecucion de giros o retiros bancarios.
 * Encapsula y valida los parametros de entrada requeridos por la operacion,
 * evitando la exposicion directa de entidades del modelo de dominio.
 */
public class SolicitudRetiroDto {

    @NotNull(message = "El identificador de la cuenta es obligatorio")
    private Long cuentaId;

    @NotNull(message = "El monto del retiro es obligatorio")
    @Min(value = 1000, message = "El monto minimo para retiros es de $1.000")
    private Long monto;

    private String canal;

    public SolicitudRetiroDto() {}

    public SolicitudRetiroDto(Long cuentaId, Long monto, String canal) {
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.canal = canal;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long cuentaId;
        private Long monto;
        private String canal;

        public Builder cuentaId(Long cuentaId) { this.cuentaId = cuentaId; return this; }
        public Builder monto(Long monto) { this.monto = monto; return this; }
        public Builder canal(String canal) { this.canal = canal; return this; }

        public SolicitudRetiroDto build() {
            return new SolicitudRetiroDto(cuentaId, monto, canal);
        }
    }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
}
