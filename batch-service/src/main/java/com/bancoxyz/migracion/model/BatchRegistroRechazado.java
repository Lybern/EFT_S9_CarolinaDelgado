package com.bancoxyz.migracion.model;

import java.time.LocalDateTime;

/**
 * Entidad/Modelo que representa un registro rechazado u omitido durante el procesamiento Batch
 * y persistido en la tabla batch_registros_rechazados para auditoría y Dead Letter Queue.
 */
public class BatchRegistroRechazado {

    private Long id;
    private String jobName;
    private String stepName;
    private String identificadorRegistro;
    private String datosOrigen;
    private String tipoError;
    private String motivoRechazo;
    private LocalDateTime fechaRegistro;

    public BatchRegistroRechazado() {
    }

    public BatchRegistroRechazado(Long id, String jobName, String stepName, String identificadorRegistro,
                                  String datosOrigen, String tipoError, String motivoRechazo, LocalDateTime fechaRegistro) {
        this.id = id;
        this.jobName = jobName;
        this.stepName = stepName;
        this.identificadorRegistro = identificadorRegistro;
        this.datosOrigen = datosOrigen;
        this.tipoError = tipoError;
        this.motivoRechazo = motivoRechazo;
        this.fechaRegistro = fechaRegistro;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getIdentificadorRegistro() {
        return identificadorRegistro;
    }

    public void setIdentificadorRegistro(String identificadorRegistro) {
        this.identificadorRegistro = identificadorRegistro;
    }

    public String getDatosOrigen() {
        return datosOrigen;
    }

    public void setDatosOrigen(String datosOrigen) {
        this.datosOrigen = datosOrigen;
    }

    public String getTipoError() {
        return tipoError;
    }

    public void setTipoError(String tipoError) {
        this.tipoError = tipoError;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return "BatchRegistroRechazado{" +
                "id=" + id +
                ", jobName='" + jobName + '\'' +
                ", stepName='" + stepName + '\'' +
                ", identificadorRegistro='" + identificadorRegistro + '\'' +
                ", datosOrigen='" + datosOrigen + '\'' +
                ", tipoError='" + tipoError + '\'' +
                ", motivoRechazo='" + motivoRechazo + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                '}';
    }

    public static class Builder {
        private Long id;
        private String jobName;
        private String stepName;
        private String identificadorRegistro;
        private String datosOrigen;
        private String tipoError;
        private String motivoRechazo;
        private LocalDateTime fechaRegistro;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder jobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public Builder stepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public Builder identificadorRegistro(String identificadorRegistro) {
            this.identificadorRegistro = identificadorRegistro;
            return this;
        }

        public Builder datosOrigen(String datosOrigen) {
            this.datosOrigen = datosOrigen;
            return this;
        }

        public Builder tipoError(String tipoError) {
            this.tipoError = tipoError;
            return this;
        }

        public Builder motivoRechazo(String motivoRechazo) {
            this.motivoRechazo = motivoRechazo;
            return this;
        }

        public Builder fechaRegistro(LocalDateTime fechaRegistro) {
            this.fechaRegistro = fechaRegistro;
            return this;
        }

        public BatchRegistroRechazado build() {
            return new BatchRegistroRechazado(id, jobName, stepName, identificadorRegistro, datosOrigen, tipoError, motivoRechazo, fechaRegistro);
        }
    }
}
