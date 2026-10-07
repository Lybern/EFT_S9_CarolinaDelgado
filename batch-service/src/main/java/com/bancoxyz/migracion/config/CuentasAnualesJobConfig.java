package com.bancoxyz.migracion.config;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.exception.CuentaAnualValidationException;
import com.bancoxyz.migracion.listener.BatchJobCompletionListener;
import com.bancoxyz.migracion.listener.BatchStepExecutionListener;
import com.bancoxyz.migracion.listener.CuentaAnualSkipListener;
import com.bancoxyz.migracion.model.CuentaAnualProcesada;
import com.bancoxyz.migracion.partitioner.CsvRangePartitioner;
import com.bancoxyz.migracion.policy.FileVerificationSkipper;
import com.bancoxyz.migracion.processor.CuentaAnualItemProcessor;
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
 * Configuración del Job 3: Generación de Estados de Cuenta Anuales (Banco XYZ) - Semana 3.
 * Implementa escalamiento mediante particiones concurrentes (gridSize=3), compilación de
 * estados financieros anuales para auditoría bancaria y tolerancia a fallos.
 */
@Configuration
public class CuentasAnualesJobConfig {

    @Value("${app.batch.cuentas-anuales-file}")
    private Resource inputResource;

    // 1. PARTICIONADOR
    @Bean
    public Partitioner cuentasAnualesPartitioner() {
        return new CsvRangePartitioner(inputResource, 1);
    }

    // 2. LECTOR PARTICIONADO (@StepScope por partición)
    @Bean
    @StepScope
    public FlatFileItemReader<CuentaAnualDTO> cuentasAnualesReader(
            @Value("#{stepExecutionContext['linesToSkip']}") Integer linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") Integer maxItemCount,
            @Value("#{stepExecutionContext['partitionName']}") String partitionName) {
        int skip = (linesToSkip != null) ? linesToSkip : 1;
        int maxCount = (maxItemCount != null) ? maxItemCount : Integer.MAX_VALUE;
        String readerName = "cuentasAnualesReader_" + (partitionName != null ? partitionName : "default");

        return new FlatFileItemReaderBuilder<CuentaAnualDTO>()
                .name(readerName)
                .resource(inputResource)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .linesToSkip(skip)
                .maxItemCount(maxCount)
                .targetType(CuentaAnualDTO.class)
                .saveState(false)
                .build();
    }

    // 3. PROCESADOR
    @Bean
    public CuentaAnualItemProcessor cuentasAnualesProcessor() {
        return new CuentaAnualItemProcessor();
    }

    // 4. ESCRITOR JDBC
    @Bean
    public JdbcBatchItemWriter<CuentaAnualProcesada> cuentasAnualesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaAnualProcesada>()
                .dataSource(dataSource)
                .sql("INSERT INTO cuentas_anuales_procesadas (" +
                     "cuenta_id, fecha, transaccion, monto, descripcion, es_anomala, motivo_observacion) " +
                     "VALUES (:cuentaId, :fecha, :transaccion, :monto, :descripcion, :esAnomala, :motivoObservacion)")
                .beanMapped()
                .build();
    }

    // 5. STEP ESCLAVO / WORKER (MinionStep)
    @Bean
    public Step cuentasAnualesMinionStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        FlatFileItemReader<CuentaAnualDTO> cuentasAnualesReader,
                                        CuentaAnualItemProcessor cuentasAnualesProcessor,
                                        JdbcBatchItemWriter<CuentaAnualProcesada> cuentasAnualesWriter,
                                        BatchStepExecutionListener stepExecutionListener,
                                        CuentaAnualSkipListener cuentaAnualSkipListener,
                                        FileVerificationSkipper fileVerificationSkipper) {
        return new StepBuilder("cuentasAnualesMinionStep", jobRepository)
                .<CuentaAnualDTO, CuentaAnualProcesada>chunk(5, transactionManager)
                .reader(cuentasAnualesReader)
                .processor(cuentasAnualesProcessor)
                .writer(cuentasAnualesWriter)
                .listener(stepExecutionListener)
                .listener((StepExecutionListener) cuentaAnualSkipListener)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .skip(FlatFileParseException.class)
                .skip(DateTimeParseException.class)
                .skip(NumberFormatException.class)
                .skip(CuentaAnualValidationException.class)
                .skipLimit(10)
                .listener((SkipListener<CuentaAnualDTO, CuentaAnualProcesada>) cuentaAnualSkipListener)
                .retryLimit(3)
                .retry(CannotAcquireLockException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retry(TransientDataAccessException.class)
                .build();
    }

    // 6. MANEJADOR DE PARTICIONES (TaskExecutorPartitionHandler con gridSize=3)
    @Bean
    public TaskExecutorPartitionHandler cuentasAnualesPartitionHandler(
            @Qualifier("cuentasAnualesMinionStep") Step cuentasAnualesMinionStep,
            @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(cuentasAnualesMinionStep);
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setGridSize(3); // 3 particiones en paralelo
        return partitionHandler;
    }

    // 7. STEP PRINCIPAL MAESTRO (PartitionStep)
    @Bean
    public Step cuentasAnualesPartitionStep(JobRepository jobRepository,
                                           @Qualifier("cuentasAnualesPartitioner") Partitioner cuentasAnualesPartitioner,
                                           @Qualifier("cuentasAnualesPartitionHandler") TaskExecutorPartitionHandler cuentasAnualesPartitionHandler) {
        return new StepBuilder("cuentasAnualesPartitionStep", jobRepository)
                .partitioner("cuentasAnualesMinionStep", cuentasAnualesPartitioner)
                .partitionHandler(cuentasAnualesPartitionHandler)
                .build();
    }

    // 8. JOB FINAL
    @Bean
    public Job cuentasAnualesJob(JobRepository jobRepository,
                                @Qualifier("cuentasAnualesPartitionStep") Step cuentasAnualesPartitionStep,
                                BatchJobCompletionListener jobCompletionListener) {
        return new JobBuilder("cuentasAnualesJob", jobRepository)
                .start(cuentasAnualesPartitionStep)
                .listener(jobCompletionListener)
                .build();
    }
}