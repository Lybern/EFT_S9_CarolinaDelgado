package com.bancoxyz.migracion.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void testParsearFechaFormatoIso() {
        LocalDate fecha = DateUtil.parsearFecha("2024-05-15");
        assertNotNull(fecha);
        assertEquals(LocalDate.of(2024, 5, 15), fecha);
    }

    @Test
    void testParsearFechaFormatoSlashYmd() {
        LocalDate fecha = DateUtil.parsearFecha("2024/08/20");
        assertNotNull(fecha);
        assertEquals(LocalDate.of(2024, 8, 20), fecha);
    }

    @Test
    void testParsearFechaFormatoDmyGuiones() {
        LocalDate fecha = DateUtil.parsearFecha("25-12-2024");
        assertNotNull(fecha);
        assertEquals(LocalDate.of(2024, 12, 25), fecha);
    }

    @Test
    void testParsearFechaFormatoDmyBarras() {
        LocalDate fecha = DateUtil.parsearFecha("31/10/2024");
        assertNotNull(fecha);
        assertEquals(LocalDate.of(2024, 10, 31), fecha);
    }

    @Test
    void testParsearFechaConEspacios() {
        LocalDate fecha = DateUtil.parsearFecha("   2024-01-01   ");
        assertNotNull(fecha);
        assertEquals(LocalDate.of(2024, 1, 1), fecha);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testParsearFechaNulaOVacia(String input) {
        assertNull(DateUtil.parsearFecha(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid_date",
            "2024-13-01",
            "2024-02-30",
            "32/01/2024",
            "20240101",
            "abc",
            "12345"
    })
    void testParsearFechaInvalidaRetornaNull(String fechaInvalida) {
        assertNull(DateUtil.parsearFecha(fechaInvalida));
        assertFalse(DateUtil.esFechaValida(fechaInvalida));
    }

    @Test
    void testEsFechaValida() {
        assertTrue(DateUtil.esFechaValida("2024-01-15"));
        assertTrue(DateUtil.esFechaValida("2024/01/15"));
        assertTrue(DateUtil.esFechaValida("15-01-2024"));
        assertTrue(DateUtil.esFechaValida("15/01/2024"));
        assertFalse(DateUtil.esFechaValida("formato-invalido"));
        assertFalse(DateUtil.esFechaValida(null));
    }

    @Test
    void testParsearFechaOptional() {
        Optional<LocalDate> optValido = DateUtil.parsearFechaOptional("2024-06-30");
        assertTrue(optValido.isPresent());
        assertEquals(LocalDate.of(2024, 6, 30), optValido.get());

        Optional<LocalDate> optInvalido = DateUtil.parsearFechaOptional("fecha_corrupta");
        assertFalse(optInvalido.isPresent());

        Optional<LocalDate> optNull = DateUtil.parsearFechaOptional(null);
        assertFalse(optNull.isPresent());
    }

    @Test
    void testFormatearFecha() {
        LocalDate fecha = LocalDate.of(2024, 7, 4);
        assertEquals("2024-07-04", DateUtil.formatearFecha(fecha, "yyyy-MM-dd"));
        assertEquals("04/07/2024", DateUtil.formatearFecha(fecha, "dd/MM/yyyy"));
        assertNull(DateUtil.formatearFecha(null, "yyyy-MM-dd"));
        assertNull(DateUtil.formatearFecha(fecha, null));
    }

    @Test
    void testFormatearTimestamp() {
        LocalDateTime ldt = LocalDateTime.of(2024, 9, 15, 14, 30, 45);
        assertEquals("2024-09-15 14:30:45", DateUtil.formatearTimestamp(ldt));
        assertNull(DateUtil.formatearTimestamp(null));
    }
}
