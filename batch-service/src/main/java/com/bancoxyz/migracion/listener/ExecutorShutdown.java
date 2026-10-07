package com.bancoxyz.migracion.listener;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Componente encargado del cierre ordenado y graceful shutdown del pool de hilos
 * al finalizar la aplicación.
 */
@Component
public class ExecutorShutdown {

    private static final Logger log = LoggerFactory.getLogger(ExecutorShutdown.class);
    private final ThreadPoolTaskExecutor taskExecutor;

    public ExecutorShutdown(@Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando ThreadPoolTaskExecutor de procesamiento Batch de manera segura...");
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
    }
}
