package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.model.TransaccionProcesada;
import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SkipListener específico para el Step de Transacciones Diarias (Semana 3).
 * Soporta ejecución particionada y auditoría de skips.
 */
@Component
public class TransaccionSkipListener extends AbstractBatchSkipListener<TransaccionDTO, TransaccionProcesada> {

    public TransaccionSkipListener() {
        super(null, "reporteTransaccionesDiariasJob", "transaccionesMinionStep");
    }

    @Autowired
    public TransaccionSkipListener(BatchRegistroRechazadoRepository repository) {
        super(repository, "reporteTransaccionesDiariasJob", "transaccionesMinionStep");
    }
}
