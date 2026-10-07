package com.bancoxyz.migracion.exception;

/**
 * Excepción para errores de validación en cálculo de intereses.
 */
public class CuentaInteresValidationException extends BatchValidationException {

    public CuentaInteresValidationException(String message) {
        super(message);
    }

    public CuentaInteresValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
