package com.bancoxyz.migracion.dto;

public class CuentaAnualDTO {
    private String cuentaId;
    private String fecha;
    private String transaccion;
    private String monto;
    private String descripcion;

    public CuentaAnualDTO() {
    }

    public CuentaAnualDTO(String cuentaId, String fecha, String transaccion, String monto, String descripcion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public String getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(String cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTransaccion() {
        return transaccion;
    }

    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "CuentaAnualDTO{" +
                "cuentaId='" + cuentaId + '\'' +
                ", fecha='" + fecha + '\'' +
                ", transaccion='" + transaccion + '\'' +
                ", monto='" + monto + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}