package com.bancoxyz.migracion.model;

import java.math.BigDecimal;

public class CuentaInteresProcesada {

    private Long id;
    private String cuentaId;
    private String nombre;
    private BigDecimal saldo;
    private String tipo;
    private BigDecimal interesCalculado;
    private BigDecimal saldoFinal;
    private boolean esAnomala;
    private String motivoObservacion;

    public CuentaInteresProcesada() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCuentaId() { return cuentaId; }
    public void setCuentaId(String cuentaId) { this.cuentaId = cuentaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { 
        this.saldo = saldo; 
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getInteresCalculado() { return interesCalculado; }
    public void setInteresCalculado(BigDecimal interesCalculado) { this.interesCalculado = interesCalculado; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public boolean getEsAnomala() { return esAnomala; }
    public boolean isEsAnomala() { return esAnomala; }
    public void setEsAnomala(boolean esAnomala) { this.esAnomala = esAnomala; }

    public String getMotivoObservacion() { return motivoObservacion; }
    public void setMotivoObservacion(String motivoObservacion) { this.motivoObservacion = motivoObservacion; }
}