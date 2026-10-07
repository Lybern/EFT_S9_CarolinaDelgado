package com.bancoxyz.migracion.exception;

/**
 * Excepción para datos inválidos o mal clasificados encontrados durante el procesamiento batch.
 */
public class InvalidDataException extends BatchValidationException {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
