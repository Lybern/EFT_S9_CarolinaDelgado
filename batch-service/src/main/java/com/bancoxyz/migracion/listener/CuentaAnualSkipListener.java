package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.model.CuentaAnualProcesada;
import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SkipListener específico para el Step de Estados de Cuenta Anuales (Semana 3).
 * Soporta ejecución particionada y auditoría de skips.
 */
@Component
public class CuentaAnualSkipListener extends AbstractBatchSkipListener<CuentaAnualDTO, CuentaAnualProcesada> {

    public CuentaAnualSkipListener() {
        super(null, "cuentasAnualesJob", "cuentasAnualesMinionStep");
    }

    @Autowired
    public CuentaAnualSkipListener(BatchRegistroRechazadoRepository repository) {
        super(repository, "cuentasAnualesJob", "cuentasAnualesMinionStep");
    }
}
