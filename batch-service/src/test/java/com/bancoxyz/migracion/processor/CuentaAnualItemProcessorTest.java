package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.exception.CuentaAnualValidationException;
import com.bancoxyz.migracion.model.CuentaAnualProcesada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CuentaAnualItemProcessorTest {

    private CuentaAnualItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CuentaAnualItemProcessor();
    }

    @Test
    void testProcesarCuentaAnualValida() throws Exception {
        CuentaAnualDTO dto = new CuentaAnualDTO("2001", "2024-03-15", "DEPOSITO", "450000.00", "Abono");
        CuentaAnualProcesada result = processor.process(dto);

        assertNotNull(result);
        assertEquals(2001L, result.getCuentaId());
        assertEquals(LocalDate.of(2024, 3, 15), result.getFecha());
        assertEquals(new BigDecimal("450000.00"), result.getMonto());
        assertEquals("DEPOSITO", result.getTransaccion());
        assertFalse(result.getEsAnomala());
    }

    @Test
    void testCuentaAnualIdNoNumericoLanzaExcepcion() {
        CuentaAnualDTO dto = new CuentaAnualDTO("INVALID_ID", "2024-03-15", "DEPOSITO", "450000.00", "Abono");
        assertThrows(CuentaAnualValidationException.class, () -> processor.process(dto));
    }

    @Test
    void testCuentaAnualMontoInvalidoLanzaExcepcion() {
        CuentaAnualDTO dto = new CuentaAnualDTO("2002", "2024-03-15", "DEPOSITO", "monto_invalido", "Abono");
        assertThrows(NumberFormatException.class, () -> processor.process(dto));
    }

    @Test
    void testCuentaAnualDuplicadaEsAnomala() throws Exception {
        CuentaAnualDTO dto1 = new CuentaAnualDTO("2003", "2024-04-10", "DEPOSITO", "120000.00", "Abono mensual");
        CuentaAnualDTO dto2 = new CuentaAnualDTO("2003", "2024-04-10", "DEPOSITO", "120000.00", "Abono mensual");

        CuentaAnualProcesada res1 = processor.process(dto1);
        CuentaAnualProcesada res2 = processor.process(dto2);

        assertNotNull(res1);
        assertNotNull(res2);
        assertFalse(res1.getEsAnomala());
        assertTrue(res2.getEsAnomala());
        assertTrue(res2.getMotivoObservacion().contains("duplicado"));
    }
}
