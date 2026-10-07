package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.exception.TransaccionValidationException;
import com.bancoxyz.migracion.model.TransaccionProcesada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransaccionItemProcessorTest {

    private TransaccionItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransaccionItemProcessor();
    }

    @Test
    void testProcesarTransaccionValida() throws Exception {
        TransaccionDTO dto = new TransaccionDTO("TX100", "2024-05-10", "150000.00", "CREDITO");
        TransaccionProcesada result = processor.process(dto);

        assertNotNull(result);
        assertEquals("TX100", result.getTransaccionId());
        assertEquals(LocalDate.of(2024, 5, 10), result.getFechaTransaccion());
        assertEquals(new BigDecimal("150000.00"), result.getMonto());
        assertEquals("CREDITO", result.getTipoTransaccion());
        assertFalse(result.isEsAnomala());
        assertNull(result.getMotivoObservacion());
    }

    @Test
    void testTransaccionConIdFaltanteLanzaExcepcion() {
        TransaccionDTO dto = new TransaccionDTO("", "2024-05-10", "150000.00", "CREDITO");
        assertThrows(TransaccionValidationException.class, () -> processor.process(dto));
    }

    @Test
    void testTransaccionConMontoInvalidoLanzaExcepcion() {
        TransaccionDTO dto = new TransaccionDTO("TX101", "2024-05-10", "abc_monto", "DEBITO");
        assertThrows(NumberFormatException.class, () -> processor.process(dto));
    }

    @Test
    void testTransaccionConMontoNegativoEsAnomala() throws Exception {
        TransaccionDTO dto = new TransaccionDTO("TX102", "2024-05-10", "-5000.00", "DEBITO");
        TransaccionProcesada result = processor.process(dto);

        assertNotNull(result);
        assertTrue(result.isEsAnomala());
        assertTrue(result.getMotivoObservacion().contains("Monto negativo"));
    }

    @Test
    void testTransaccionDuplicadaEsAnomala() throws Exception {
        TransaccionDTO dto1 = new TransaccionDTO("TX103", "2024-05-10", "80000.00", "CREDITO");
        TransaccionDTO dto2 = new TransaccionDTO("TX103", "2024-05-10", "80000.00", "CREDITO");

        TransaccionProcesada res1 = processor.process(dto1);
        TransaccionProcesada res2 = processor.process(dto2);

        assertFalse(res1.isEsAnomala());
        assertTrue(res2.isEsAnomala());
        assertTrue(res2.getMotivoObservacion().contains("duplicado"));
    }
}
