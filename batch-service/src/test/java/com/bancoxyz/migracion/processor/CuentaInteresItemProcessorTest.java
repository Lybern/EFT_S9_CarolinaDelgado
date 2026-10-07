package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.exception.CuentaInteresValidationException;
import com.bancoxyz.migracion.model.CuentaInteresProcesada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CuentaInteresItemProcessorTest {

    private CuentaInteresItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CuentaInteresItemProcessor();
    }

    @Test
    void testCalculoInteresValido() throws Exception {
        CuentaInteresDTO dto = new CuentaInteresDTO("CTA-500", "Carlos Perez", "1200000.00", "30", "AHORRO");
        CuentaInteresProcesada result = processor.process(dto);

        assertNotNull(result);
        assertEquals("CTA-500", result.getCuentaId());
        assertEquals("Carlos Perez", result.getNombre());
        // Interés mensual = (1,200,000 * 5%) / 12 = 60,000 / 12 = 5000.00
        assertEquals(new BigDecimal("5000.00"), result.getInteresCalculado());
        assertEquals(new BigDecimal("1205000.00"), result.getSaldoFinal());
        assertFalse(result.isEsAnomala());
    }

    @Test
    void testCuentaSinIdLanzaExcepcion() {
        CuentaInteresDTO dto = new CuentaInteresDTO("", "Ana Rojas", "500000.00", "25", "AHORRO");
        assertThrows(CuentaInteresValidationException.class, () -> processor.process(dto));
    }

    @Test
    void testCuentaConSaldoNegativoEsAnomala() throws Exception {
        CuentaInteresDTO dto = new CuentaInteresDTO("CTA-501", "Pedro Soto", "-10000.00", "40", "AHORRO");
        CuentaInteresProcesada result = processor.process(dto);

        assertNotNull(result);
        assertTrue(result.isEsAnomala());
        assertEquals(BigDecimal.ZERO, result.getInteresCalculado());
    }

    @Test
    void testCuentaConEdadInvalidaLanzaExcepcion() {
        CuentaInteresDTO dto = new CuentaInteresDTO("CTA-502", "Laura Diaz", "500000.00", "no_edad", "AHORRO");
        assertThrows(NumberFormatException.class, () -> processor.process(dto));
    }
}
