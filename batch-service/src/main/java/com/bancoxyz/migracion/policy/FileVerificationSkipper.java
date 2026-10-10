package com.bancoxyz.migracion.policy;

import com.bancoxyz.migracion.exception.BatchValidationException;
import com.bancoxyz.migracion.exception.InvalidDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;

/**
 * Política personalizada de tolerancia a fallos (SkipPolicy) que define qué excepciones
 * pueden omitirse y hasta qué límite de ocurrencias antes de interrumpir el Step.
 */
@Component
public class FileVerificationSkipper implements SkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(FileVerificationSkipper.class);
    private final int skipLimit;

    public FileVerificationSkipper() {
        this(500); // Límite amplio para procesar datasets completos de 1000 registros con anomalías
    }

    public FileVerificationSkipper(int skipLimit) {
        this.skipLimit = skipLimit;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (skipCount >= skipLimit) {
            log.error("Se ha alcanzado el límite máximo de omisiones permitidas (skipLimit={}). Abortando Step.", skipLimit);
            return false;
        }

        if (t instanceof FlatFileParseException) {
            FlatFileParseException ffpe = (FlatFileParseException) t;
            log.info("Error: FlatFileParseException");
            log.warn("CustomSkipPolicy - Excepción omitida: Parsing error at line: {} in resource=[{}], input=[{}]",
                    ffpe.getLineNumber(), ffpe.getInput(), ffpe.getInput());
            return true;
        }

        if (t instanceof DateTimeParseException) {
            log.info("Error: DateTimeParseException");
            log.warn("CustomSkipPolicy - Excepción omitida por formato de fecha inválido: {}", t.getMessage());
            return true;
        }

        if (t instanceof NumberFormatException) {
            log.info("Error: NumberFormatException");
            log.warn("CustomSkipPolicy - Excepción omitida por formato numérico inválido: {}", t.getMessage());
            return true;
        }

        if (t instanceof InvalidDataException || t instanceof BatchValidationException) {
            log.info("Error: {}", t.getClass().getSimpleName());
            log.warn("CustomSkipPolicy - Excepción omitida: {}", t.getMessage());
            return true;
        }

        if (t instanceof IllegalArgumentException) {
            log.info("Error: IllegalArgumentException");
            log.warn("CustomSkipPolicy - Excepción omitida por argumento inválido: {}", t.getMessage());
            return true;
        }

        log.error("Excepción no omitible detectada: {} - {}", t.getClass().getName(), t.getMessage());
        return false;
    }
}
