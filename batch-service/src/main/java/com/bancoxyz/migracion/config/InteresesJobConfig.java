package com.bancoxyz.migracion.config;

import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.exception.CuentaInteresValidationException;
import com.bancoxyz.migracion.listener.BatchJobCompletionListener;
import com.bancoxyz.migracion.listener.BatchStepExecutionListener;
import com.bancoxyz.migracion.listener.CuentaInteresSkipListener;
import com.bancoxyz.migracion.model.CuentaInteresProcesada;
import com.bancoxyz.migracion.partitioner.CsvRangePartitioner;
import com.bancoxyz.migracion.policy.FileVerificationSkipper;
import com.bancoxyz.migracion.processor.CuentaInteresItemProcessor;
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

/**
 * Configuración del Job 2: Cálculo de Intereses Mensuales (Banco XYZ) - Semana 3.
 * Implementa arquitectura de escalado mediante particiones distribuidas (gridSize=3),
 * cálculo financiero bancario de tasas de interés mensuales, tolerancia a fallos
 * mediante FileVerificationSkipper y persistencia en tabla intereses_procesados.
 */
@Configuration
public class InteresesJobConfig {

    @Value("${app.batch.intereses-file}")
    private Resource inputResource;

    // 1. PARTICIONADOR
    @Bean
    public Partitioner interesesPartitioner() {
        return new CsvRangePartitioner(inputResource, 1);
    }

    // 2. LECTOR PARTICIONADO (@StepScope por partición)
    @Bean
    @StepScope
    public FlatFileItemReader<CuentaInteresDTO> interesesReader(
            @Value("#{stepExecutionContext['linesToSkip']}") Integer linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") Integer maxItemCount,
            @Value("#{stepExecutionContext['partitionName']}") String partitionName) {
        int skip = (linesToSkip != null) ? linesToSkip : 1;
        int maxCount = (maxItemCount != null) ? maxItemCount : Integer.MAX_VALUE;
        String readerName = "interesesReader_" + (partitionName != null ? partitionName : "default");

        return new FlatFileItemReaderBuilder<CuentaInteresDTO>()
                .name(readerName)
                .resource(inputResource)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .linesToSkip(skip)
                .maxItemCount(maxCount)
                .targetType(CuentaInteresDTO.class)
                .saveState(false)
                .build();
    }

    // 3. PROCESADOR
    @Bean
    public CuentaInteresItemProcessor interesesProcessor() {
        return new CuentaInteresItemProcessor();
    }

    // 4. ESCRITOR JDBC
    @Bean
    public JdbcBatchItemWriter<CuentaInteresProcesada> interesesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaInteresProcesada>()
                .dataSource(dataSource)
                .sql("INSERT INTO intereses_procesados (" +
                     "cuenta_id, nombre, saldo, tipo, interes_calculado, saldo_final, es_anomala, motivo_observacion) " +
                     "VALUES (:cuentaId, :nombre, :saldo, :tipo, :interesCalculado, :saldoFinal, :esAnomala, :motivoObservacion)")
                .beanMapped()
                .build();
    }

    // 5. STEP ESCLAVO / WORKER (MinionStep)
    @Bean
    public Step interesesMinionStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   FlatFileItemReader<CuentaInteresDTO> interesesReader,
                                   CuentaInteresItemProcessor interesesProcessor,
                                   JdbcBatchItemWriter<CuentaInteresProcesada> interesesWriter,
                                   BatchStepExecutionListener stepExecutionListener,
                                   CuentaInteresSkipListener cuentaInteresSkipListener,
                                   FileVerificationSkipper fileVerificationSkipper) {
        return new StepBuilder("interesesMinionStep", jobRepository)
                .<CuentaInteresDTO, CuentaInteresProcesada>chunk(5, transactionManager)
                .reader(interesesReader)
                .processor(interesesProcessor)
                .writer(interesesWriter)
                .listener(stepExecutionListener)
                .listener((StepExecutionListener) cuentaInteresSkipListener)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skip(CuentaInteresValidationException.class)
                .skipLimit(500)
                .listener((SkipListener<CuentaInteresDTO, CuentaInteresProcesada>) cuentaInteresSkipListener)
                .retryLimit(3)
                .retry(CannotAcquireLockException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retry(TransientDataAccessException.class)
                .build();
    }

    // 6. MANEJADOR DE PARTICIONES (TaskExecutorPartitionHandler con gridSize=3)
    @Bean
    public TaskExecutorPartitionHandler interesesPartitionHandler(
            @Qualifier("interesesMinionStep") Step interesesMinionStep,
            @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(interesesMinionStep);
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setGridSize(3); // 3 particiones en paralelo
        return partitionHandler;
    }

    // 7. STEP PRINCIPAL MAESTRO (PartitionStep)
    @Bean
    public Step interesesPartitionStep(JobRepository jobRepository,
                                       @Qualifier("interesesPartitioner") Partitioner interesesPartitioner,
                                       @Qualifier("interesesPartitionHandler") TaskExecutorPartitionHandler interesesPartitionHandler) {
        return new StepBuilder("interesesPartitionStep", jobRepository)
                .partitioner("interesesMinionStep", interesesPartitioner)
                .partitionHandler(interesesPartitionHandler)
                .build();
    }

    // 8. JOB FINAL
    @Bean
    public Job calculoInteresesJob(JobRepository jobRepository,
                                  @Qualifier("interesesPartitionStep") Step interesesPartitionStep,
                                  BatchJobCompletionListener jobCompletionListener) {
        return new JobBuilder("calculoInteresesJob", jobRepository)
                .start(interesesPartitionStep)
                .listener(jobCompletionListener)
                .build();
    }
}