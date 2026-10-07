package com.bancoxyz.migracion.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Listener encargado de monitorear el ciclo de vida de cada Step, midiendo tiempos
 * de ejecución en milisegundos, registros procesados, omitidos y estado de salida.
 */
@Component
public class BatchStepExecutionListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchStepExecutionListener.class);
    private long startTime;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.startTime = System.currentTimeMillis();
        log.info("================================================================================");
        log.info("[{}] Iniciando Step: '{}' (Job ID: {}) en Hilo: [{}]",
                Thread.currentThread().getName(),
                stepExecution.getStepName(),
                stepExecution.getJobExecutionId(),
                Thread.currentThread().getName());
        log.info("================================================================================");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long duration = System.currentTimeMillis() - this.startTime;
        log.info("--------------------------------------------------------------------------------");
        log.info("Step: [{}] ejecutado en {} ms", stepExecution.getStepName(), duration);
        log.info("  -> Registros leídos (ReadCount):       {}", stepExecution.getReadCount());
        log.info("  -> Registros procesados (WriteCount):  {}", stepExecution.getWriteCount());
        log.info("  -> Registros filtrados (FilterCount):  {}", stepExecution.getFilterCount());
        log.info("  -> Registros omitidos (SkipCount):     {}", stepExecution.getSkipCount());
        log.info("  -> Conteo de Chunks (CommitCount):     {}", stepExecution.getCommitCount());
        log.info("  -> Conteo de Rollbacks:                {}", stepExecution.getRollbackCount());
        log.info("  -> Estado de Salida del Step:          {}", stepExecution.getExitStatus().getExitCode());
        log.info("--------------------------------------------------------------------------------");

        return stepExecution.getExitStatus();
    }
}
