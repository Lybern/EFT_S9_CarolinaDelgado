package cl.duoc.bancoxyz.core.exception;

import java.time.LocalDateTime;

public class ErrorRespuestaDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private String path;

    public ErrorRespuestaDto() {}

    public ErrorRespuestaDto(LocalDateTime timestamp, int status, String error, String mensaje, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String mensaje;
        private String path;

        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder status(int status) { this.status = status; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }
        public Builder path(String path) { this.path = path; return this; }

        public ErrorRespuestaDto build() {
            return new ErrorRespuestaDto(timestamp, status, error, mensaje, path);
        }
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
