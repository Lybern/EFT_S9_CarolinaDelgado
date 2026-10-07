package com.bancoxyz.migracion.config;

import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.exception.TransaccionValidationException;
import com.bancoxyz.migracion.listener.BatchJobCompletionListener;
import com.bancoxyz.migracion.listener.BatchStepExecutionListener;
import com.bancoxyz.migracion.listener.TransaccionSkipListener;
import com.bancoxyz.migracion.model.TransaccionProcesada;
import com.bancoxyz.migracion.partitioner.CsvRangePartitioner;
import com.bancoxyz.migracion.policy.FileVerificationSkipper;
import com.bancoxyz.migracion.processor.TransaccionItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.format.DateTimeParseException;

/**
 * Configuración del Job 1: Reporte de Transacciones Diarias (Banco XYZ) - Semana 3.
 * Implementa arquitectura escalable con Particiones (PartitionStep + MinionSteps paralelos),
 * manejador de particiones TaskExecutorPartitionHandler (gridSize=3), tolerancia a fallos
 * personalizada (FileVerificationSkipper) y reintentos ante bloqueos de base de datos.
 */
@Configuration
public class TransaccionesJobConfig {

    @Value("${app.batch.input-file}")
    private Resource inputResource;

    // 1. PARTICIONADOR
    @Bean
    public Partitioner transaccionesPartitioner() {
        return new CsvRangePartitioner(inputResource, 1);
    }

    // 2. LECTOR PARTICIONADO (@StepScope para instanciación thread-safe por partición)
    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionDTO> transaccionReader(
            @Value("#{stepExecutionContext['linesToSkip']}") Integer linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") Integer maxItemCount,
            @Value("#{stepExecutionContext['partitionName']}") String partitionName) {
        int skip = (linesToSkip != null) ? linesToSkip : 1;
        int maxCount = (maxItemCount != null) ? maxItemCount : Integer.MAX_VALUE;
        String readerName = "transaccionReader_" + (partitionName != null ? partitionName : "default");

        return new FlatFileItemReaderBuilder<TransaccionDTO>()
                .name(readerName)
                .resource(inputResource)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .linesToSkip(skip)
                .maxItemCount(maxCount)
                .targetType(TransaccionDTO.class)
                .saveState(false)
                .build();
    }

    // 3. PROCESADOR
    @Bean
    public TransaccionItemProcessor transaccionProcessor() {
        return new TransaccionItemProcessor();
    }

    // 4. ESCRITOR JDBC
    @Bean
    public JdbcBatchItemWriter<TransaccionProcesada> transaccionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionProcesada>()
                .dataSource(dataSource)
                .sql("INSERT INTO transacciones_procesadas (" +
                     "transaccion_id, fecha_transaccion, monto, tipo_transaccion, es_anomala, motivo_observacion) " +
                     "VALUES (:transaccionId, :fechaTransaccion, :monto, :tipoTransaccion, :esAnomala, :motivoObservacion)")
                .beanMapped()
                .build();
    }

    // 5. STEP ESCLAVO / WORKER (MinionStep ejecutado en paralelo por cada partición)
    @Bean
    public Step transaccionesMinionStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        FlatFileItemReader<TransaccionDTO> transaccionReader,
                                        TransaccionItemProcessor transaccionProcessor,
                                        JdbcBatchItemWriter<TransaccionProcesada> transaccionWriter,
                                        BatchStepExecutionListener stepExecutionListener,
                                        TransaccionSkipListener transaccionSkipListener,
                                        FileVerificationSkipper fileVerificationSkipper) {
        return new StepBuilder("transaccionesMinionStep", jobRepository)
                .<TransaccionDTO, TransaccionProcesada>chunk(5, transactionManager)
                .reader(transaccionReader)
                .processor(transaccionProcessor)
                .writer(transaccionWriter)
                .listener(stepExecutionListener)
                .listener((StepExecutionListener) transaccionSkipListener)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .skip(FlatFileParseException.class)
                .skip(DateTimeParseException.class)
                .skip(NumberFormatException.class)
                .skip(TransaccionValidationException.class)
                .skipLimit(10)
                .listener((SkipListener<TransaccionDTO, TransaccionProcesada>) transaccionSkipListener)
                .retryLimit(3)
                .retry(CannotAcquireLockException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retry(TransientDataAccessException.class)
                .build();
    }

    // 6. MANEJADOR DE PARTICIONES (TaskExecutorPartitionHandler con gridSize=3)
    @Bean
    public TaskExecutorPartitionHandler transaccionesPartitionHandler(
            @Qualifier("transaccionesMinionStep") Step transaccionesMinionStep,
            @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(transaccionesMinionStep);
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setGridSize(3); // 3 particiones en paralelo
        return partitionHandler;
    }

    // 7. STEP PRINCIPAL MAESTRO (PartitionStep)
    @Bean
    public Step transaccionesPartitionStep(JobRepository jobRepository,
                                           @Qualifier("transaccionesPartitioner") Partitioner transaccionesPartitioner,
                                           @Qualifier("transaccionesPartitionHandler") TaskExecutorPartitionHandler transaccionesPartitionHandler) {
        return new StepBuilder("transaccionesPartitionStep", jobRepository)
                .partitioner("transaccionesMinionStep", transaccionesPartitioner)
                .partitionHandler(transaccionesPartitionHandler)
                .build();
    }

    // 8. JOB FINAL
    @Bean
    public Job reporteTransaccionesDiariasJob(JobRepository jobRepository,
                                             @Qualifier("transaccionesPartitionStep") Step transaccionesPartitionStep,
                                             BatchJobCompletionListener jobCompletionListener) {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .start(transaccionesPartitionStep)
                .listener(jobCompletionListener)
                .build();
    }
}