package com.bancoxyz.migracion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransaccionProcesada {

    private Long id;
    private String transaccionId;
    private LocalDate fechaTransaccion; 
    private BigDecimal monto;
    private String tipoTransaccion;
    private boolean esAnomala;
    private String motivoObservacion;

    public TransaccionProcesada() {}

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getTransaccionId() { 
        return transaccionId; 
    }
    public void setTransaccionId(String transaccionId) { 
        this.transaccionId = transaccionId; 
    }

    public LocalDate getFechaTransaccion() { 
        return fechaTransaccion; 
    }
    public void setFechaTransaccion(LocalDate fechaTransaccion) { 
        this.fechaTransaccion = fechaTransaccion; 
    }

    public BigDecimal getMonto() { 
        return monto; 
    }
    public void setMonto(BigDecimal monto) { 
        this.monto = monto; 
    }

    public String getTipoTransaccion() { 
        return tipoTransaccion; 
    }
    public void setTipoTransaccion(String tipoTransaccion) { 
        this.tipoTransaccion = tipoTransaccion; 
    }

    public boolean getEsAnomala() { 
        return esAnomala; 
    }
    public boolean isEsAnomala() { 
        return esAnomala; 
    }
    public void setEsAnomala(boolean esAnomala) { 
        this.esAnomala = esAnomala; 
    }

    public String getMotivoObservacion() { 
        return motivoObservacion; 
    }
    public void setMotivoObservacion(String motivoObservacion) { 
        this.motivoObservacion = motivoObservacion; 
    }
}