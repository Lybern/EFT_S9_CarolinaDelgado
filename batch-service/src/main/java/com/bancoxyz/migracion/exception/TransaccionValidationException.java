package com.bancoxyz.migracion.exception;

/**
 * Excepción para errores de validación en transacciones diarias.
 */
public class TransaccionValidationException extends BatchValidationException {

    public TransaccionValidationException(String message) {
        super(message);
    }

    public TransaccionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
