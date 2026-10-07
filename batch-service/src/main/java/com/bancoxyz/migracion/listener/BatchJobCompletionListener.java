package com.bancoxyz.migracion.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Listener encargado de monitorear el ciclo de vida de los Jobs en Spring Batch,
 * generando trazas de auditoría antes del inicio y tras la finalización de cada proceso.
 */
@Component
public class BatchJobCompletionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchJobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("================================================================================");
        log.info("[{}] Inicio del trabajo batch '{}' con ID: {}",
                Thread.currentThread().getName(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId());
        log.info("Parámetros: {}", jobExecution.getJobParameters());
        log.info("================================================================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long startTime = jobExecution.getStartTime() != null ? jobExecution.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis();
        long endTime = jobExecution.getEndTime() != null ? jobExecution.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis();
        long duration = Math.max(0, endTime - startTime);

        log.info("================================================================================");
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("[{}] Job [{}] completado exitosamente con ID: {} (Duración: {} ms)",
                    Thread.currentThread().getName(),
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getId(),
                    duration);
        } else {
            log.error("[{}] Job [{}] finalizó con anomalías o fallo. Estado: {} (ID: {})",
                    Thread.currentThread().getName(),
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus(),
                    jobExecution.getId());
        }

        log.info("Resumen del Job: status={}, exitStatus={}, duracion={} ms",
                jobExecution.getStatus(),
                jobExecution.getExitStatus().getExitCode(),
                duration);
        log.info("================================================================================");
    }
}
