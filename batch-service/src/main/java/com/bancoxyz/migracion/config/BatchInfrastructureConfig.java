package com.bancoxyz.migracion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración de infraestructura y concurrencia para Spring Batch.
 * Configura políticas de escalamiento mediante un pool de hilos optimizado (3 hilos en paralelo).
 */
@Configuration
public class BatchInfrastructureConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchInfrastructureConfig.class);

    @Bean(name = "batchTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        log.info("Configurando ThreadPoolTaskExecutor: CorePoolSize=3, MaxPoolSize=10, QueueCapacity=25, Prefix='Batch-Thread-'");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);          // 3 hilos de ejecución paralela requeridos
        executor.setMaxPoolSize(10);         // Máximo de hilos bajo alta demanda
        executor.setQueueCapacity(25);       // Capacidad de cola para chunks pendientes
        executor.setThreadNamePrefix("Batch-Thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
