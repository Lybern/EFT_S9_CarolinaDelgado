package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener genérico para registrar omisiones (skips) ocurridas durante las fases de lectura,
 * procesamiento o escritura del Batch. Genera trazabilidad en consola, persiste errores en errores.csv
 * y en la tabla batch_registros_rechazados.
 */
@Component
public class BatchSkipListener extends AbstractBatchSkipListener<Object, Object> {

    public BatchSkipListener() {
        super();
    }

    @Autowired
    public BatchSkipListener(BatchRegistroRechazadoRepository repository) {
        super(repository);
    }
}
