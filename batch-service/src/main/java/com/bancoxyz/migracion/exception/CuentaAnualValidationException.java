package com.bancoxyz.migracion.exception;

/**
 * Excepción para errores de validación en estados de cuenta anuales.
 */
public class CuentaAnualValidationException extends BatchValidationException {

    public CuentaAnualValidationException(String message) {
        super(message);
    }

    public CuentaAnualValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
