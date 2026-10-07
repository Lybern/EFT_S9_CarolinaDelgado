package com.bancoxyz.migracion;

import com.bancoxyz.migracion.benchmark.BatchScalingBenchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Runner principal de la aplicación Spring Batch (Semana 3 - Banco XYZ).
 * Ejecuta los tres Jobs particionados (reporteTransaccionesDiariasJob, calculoInteresesJob, cuentasAnualesJob),
 * consolida métricas de rendimiento por partición, audita la persistencia en Base de Datos
 * y ejecuta benchmarks de escalamiento y análisis de parámetros óptimos.
 */
@Component
public class BatchJobRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchJobRunner.class);

    private final JobLauncher jobLauncher;
    private final Job transaccionesJob;
    private final Job interesesJob;
    private final Job cuentasAnualesJob;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final BatchScalingBenchmark benchmark;

    @Value("${app.batch.run-benchmark:false}")
    private boolean runBenchmarkProperty;

    public BatchJobRunner(JobLauncher jobLauncher,
                          @Qualifier("reporteTransaccionesDiariasJob") Job transaccionesJob,
                          @Qualifier("calculoInteresesJob") Job interesesJob,
                          @Qualifier("cuentasAnualesJob") Job cuentasAnualesJob,
                          @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
                          JdbcTemplate jdbcTemplate,
                          BatchScalingBenchmark benchmark) {
        this.jobLauncher = jobLauncher;
        this.transaccionesJob = transaccionesJob;
        this.interesesJob = interesesJob;
        this.cuentasAnualesJob = cuentasAnualesJob;
        this.taskExecutor = taskExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.benchmark = benchmark;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean shouldRunBenchmark = runBenchmarkProperty || (args != null && Arrays.asList(args).contains("--benchmark"));

        log.info("==========================================================================================");
        log.info(">>> BANCO XYZ - SISTEMA DE MIGRACIÓN BATCH (SEMANA 3: PARTICIONES Y ESCALABILIDAD)");
        log.info(">>> Arquitectura: PartitionStep + MinionSteps | Handler: TaskExecutorPartitionHandler");
        log.info(">>> Concurrencia: 3 Particiones / 3 Hilos paralelos | Chunks: 5 registros | SkipLimit: 10");
        log.info("==========================================================================================");

        long globalStart = System.currentTimeMillis();

        // 1. Ejecución de los 3 Jobs de migración particionados
        ejecutarJob(transaccionesJob);
        ejecutarJob(interesesJob);
        ejecutarJob(cuentasAnualesJob);

        long globalDuration = System.currentTimeMillis() - globalStart;

        // 2. Auditoría en Base de Datos
        log.info("==========================================================================================");
        log.info(">>> AUDITORÍA DE RESULTADOS PERSISTIDOS EN BASE DE DATOS");
        log.info("==========================================================================================");
        mostrarResumenBaseDatos();

        // 3. Benchmark de Parámetros y Escalamiento
        if (shouldRunBenchmark) {
            benchmark.ejecutarComparativa();
        } else {
            log.info("------------------------------------------------------------------------------------------");
            log.info(">>> NOTA: Ejecute con '--benchmark' o 'app.batch.run-benchmark=true' para ver la matriz completa de comparativa de parámetros.");
            log.info("------------------------------------------------------------------------------------------");
        }

        log.info("==========================================================================================");
        log.info(">>> TODAS LAS MIGRACIONES BATCH FINALIZARON CON ÉXITO EN {} ms", globalDuration);
        log.info("==========================================================================================");
    }

    private void ejecutarJob(Job job) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        log.info("------------------------------------------------------------------------------------------");
        log.info(">>> Iniciando Ejecución de Job Particionado: {}", job.getName());
        long jobStart = System.currentTimeMillis();
        JobExecution execution = jobLauncher.run(job, params);
        long jobDuration = System.currentTimeMillis() - jobStart;

        log.info(">>> RESUMEN DE EJECUCIÓN - JOB: {}", job.getName());
        log.info("    Estado Final:           {}", execution.getStatus());
        log.info("    Exit Code:              {}", execution.getExitStatus().getExitCode());
        log.info("    Tiempo Total Ejecución: {} ms", jobDuration);

        for (StepExecution stepExecution : execution.getStepExecutions()) {
            log.info("    -> Step:                      {}", stepExecution.getStepName());
            log.info("       Registros Leídos (Read):   {}", stepExecution.getReadCount());
            log.info("       Registros Escritos (Write):{}", stepExecution.getWriteCount());
            log.info("       Registros Filtrados:       {}", stepExecution.getFilterCount());
            log.info("       Registros Omitidos (Skip): {}", stepExecution.getSkipCount());
            log.info("       Conteo de Chunks (Commit): {}", stepExecution.getCommitCount());
            log.info("       Estado del Step:           {}", stepExecution.getStatus());
        }

        if (execution.getStatus() == BatchStatus.COMPLETED) {
            log.info(">>> Job [{}] finalizado con ÉXITO.", job.getName());
        } else {
            log.warn(">>> Job [{}] finalizó con estado: {}", job.getName(), execution.getStatus());
        }
        log.info("------------------------------------------------------------------------------------------");
    }

    private void mostrarResumenBaseDatos() {
        try {
            Integer countTx = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transacciones_procesadas", Integer.class);
            Integer anomTx = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transacciones_procesadas WHERE es_anomala = true", Integer.class);
            log.info("TABLA transacciones_procesadas:   {} registros totales ({} anómalos clasificados)", countTx, anomTx);

            Integer countInt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intereses_procesados", Integer.class);
            Integer anomInt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intereses_procesados WHERE es_anomala = true", Integer.class);
            log.info("TABLA intereses_procesados:       {} registros totales ({} anómalos clasificados)", countInt, anomInt);

            Integer countCta = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cuentas_anuales_procesadas", Integer.class);
            Integer anomCta = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cuentas_anuales_procesadas WHERE es_anomala = true", Integer.class);
            log.info("TABLA cuentas_anuales_procesadas: {} registros totales ({} anómalos clasificados)", countCta, anomCta);

            Integer countRech = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch_registros_rechazados", Integer.class);
            log.info("TABLA batch_registros_rechazados: {} registros rechazados auditados en BD (Dead Letter Queue)", countRech);

            if (countRech != null && countRech > 0) {
                List<Map<String, Object>> rechazos = jdbcTemplate.queryForList(
                        "SELECT job_name, step_name, identificador_registro, tipo_error, motivo_rechazo FROM batch_registros_rechazados LIMIT 10");
                for (Map<String, Object> r : rechazos) {
                    log.info("  -> Rechazo BD: job={}, step={}, id={}, error={}, motivo={}",
                            r.get("job_name"), r.get("step_name"), r.get("identificador_registro"), r.get("tipo_error"), r.get("motivo_rechazo"));
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar el resumen de base de datos: {}", e.getMessage());
        }
    }
}