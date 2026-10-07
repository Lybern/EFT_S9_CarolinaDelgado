package com.bancoxyz.migracion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CuentaAnualProcesada {
    private Long cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private BigDecimal monto;
    private String descripcion;
    private Boolean esAnomala;
    private String motivoObservacion;

    public CuentaAnualProcesada() {
    }

    public CuentaAnualProcesada(Long cuentaId, LocalDate fecha, String transaccion, 
                                BigDecimal monto, String descripcion, Boolean esAnomala, 
                                String motivoObservacion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
        this.esAnomala = esAnomala;
        this.motivoObservacion = motivoObservacion;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTransaccion() {
        return transaccion;
    }

    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEsAnomala() {
        return esAnomala;
    }

    public void setEsAnomala(Boolean esAnomala) {
        this.esAnomala = esAnomala;
    }

    public String getMotivoObservacion() {
        return motivoObservacion;
    }

    public void setMotivoObservacion(String motivoObservacion) {
        this.motivoObservacion = motivoObservacion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long cuentaId;
        private LocalDate fecha;
        private String transaccion;
        private BigDecimal monto;
        private String descripcion;
        private Boolean esAnomala;
        private String motivoObservacion;

        public Builder cuentaId(Long cuentaId) {
            this.cuentaId = cuentaId;
            return this;
        }

        public Builder fecha(LocalDate fecha) {
            this.fecha = fecha;
            return this;
        }

        public Builder transaccion(String transaccion) {
            this.transaccion = transaccion;
            return this;
        }

        public Builder monto(BigDecimal monto) {
            this.monto = monto;
            return this;
        }

        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public Builder esAnomala(Boolean esAnomala) {
            this.esAnomala = esAnomala;
            return this;
        }

        public Builder motivoObservacion(String motivoObservacion) {
            this.motivoObservacion = motivoObservacion;
            return this;
        }

        public CuentaAnualProcesada build() {
            return new CuentaAnualProcesada(cuentaId, fecha, transaccion, monto, descripcion, esAnomala, motivoObservacion);
        }
    }
}