package com.bancoxyz.migracion.exception;

/**
 * Excepción base para errores de validación de datos en procesos Batch.
 */
public class BatchValidationException extends RuntimeException {

    public BatchValidationException(String message) {
        super(message);
    }

    public BatchValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
