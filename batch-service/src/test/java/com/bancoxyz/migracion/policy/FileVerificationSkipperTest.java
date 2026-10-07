package com.bancoxyz.migracion.policy;

import com.bancoxyz.migracion.exception.TransaccionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.FlatFileParseException;

import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class FileVerificationSkipperTest {

    private FileVerificationSkipper skipper;

    @BeforeEach
    void setUp() {
        skipper = new FileVerificationSkipper(10);
    }

    @Test
    void testShouldSkipFlatFileParseException() {
        FlatFileParseException ex = new FlatFileParseException("Error parseo", "linea,invalida", 5);
        assertTrue(skipper.shouldSkip(ex, 0));
        assertTrue(skipper.shouldSkip(ex, 5));
    }

    @Test
    void testShouldSkipDateTimeParseException() {
        DateTimeParseException ex = new DateTimeParseException("Error fecha", "2024-invalid", 0);
        assertTrue(skipper.shouldSkip(ex, 1));
    }

    @Test
    void testShouldSkipNumberFormatException() {
        NumberFormatException ex = new NumberFormatException("Error numero");
        assertTrue(skipper.shouldSkip(ex, 2));
    }

    @Test
    void testShouldSkipCustomValidationException() {
        TransaccionValidationException ex = new TransaccionValidationException("ID requerido");
        assertTrue(skipper.shouldSkip(ex, 3));
    }

    @Test
    void testShouldNotSkipWhenLimitExceeded() {
        NumberFormatException ex = new NumberFormatException("Error numero");
        assertFalse(skipper.shouldSkip(ex, 10));
        assertFalse(skipper.shouldSkip(ex, 15));
    }

    @Test
    void testShouldNotSkipUnmanagedException() {
        NullPointerException ex = new NullPointerException("Null inesperado");
        assertFalse(skipper.shouldSkip(ex, 0));
    }
}
