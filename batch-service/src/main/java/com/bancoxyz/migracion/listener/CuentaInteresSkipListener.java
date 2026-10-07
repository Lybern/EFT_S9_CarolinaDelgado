package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.model.CuentaInteresProcesada;
import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SkipListener específico para el Step de Cálculo de Intereses (Semana 3).
 * Soporta ejecución particionada y auditoría de skips.
 */
@Component
public class CuentaInteresSkipListener extends AbstractBatchSkipListener<CuentaInteresDTO, CuentaInteresProcesada> {

    public CuentaInteresSkipListener() {
        super(null, "calculoInteresesJob", "interesesMinionStep");
    }

    @Autowired
    public CuentaInteresSkipListener(BatchRegistroRechazadoRepository repository) {
        super(repository, "calculoInteresesJob", "interesesMinionStep");
    }
}
