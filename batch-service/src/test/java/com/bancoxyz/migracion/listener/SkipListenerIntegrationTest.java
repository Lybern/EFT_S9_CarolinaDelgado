package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.exception.CuentaAnualValidationException;
import com.bancoxyz.migracion.exception.CuentaInteresValidationException;
import com.bancoxyz.migracion.model.BatchRegistroRechazado;
import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkipListenerIntegrationTest {

    @Autowired
    private TransaccionSkipListener transaccionSkipListener;

    @Autowired
    private CuentaInteresSkipListener cuentaInteresSkipListener;

    @Autowired
    private CuentaAnualSkipListener cuentaAnualSkipListener;

    @Autowired
    private BatchRegistroRechazadoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM batch_registros_rechazados");
    }

    @Test
    void testTransaccionSkipListenerOnProcessError() {
        TransaccionDTO dto = new TransaccionDTO("TX_ERR_01", "2024-01-01", "invalid", "DEBITO");
        NumberFormatException ex = new NumberFormatException("Formato no numérico");

        transaccionSkipListener.onSkipInProcess(dto, ex);

        assertEquals(1, repository.contarRegistrosRechazados());
        List<BatchRegistroRechazado> rechazos = repository.obtenerTodos();
        BatchRegistroRechazado r = rechazos.get(0);

        assertEquals("reporteTransaccionesDiariasJob", r.getJobName());
        assertTrue(r.getStepName().contains("transacciones"));
        assertEquals("TX_ERR_01", r.getIdentificadorRegistro());
        assertEquals("NumberFormatException", r.getTipoError());
        assertEquals("Formato no numérico", r.getMotivoRechazo());
    }

    @Test
    void testTransaccionSkipListenerOnReadError() {
        FlatFileParseException ffpe = new FlatFileParseException("Error de parseo CSV", "TX_CORRUPTA,invalid,data", 15);

        transaccionSkipListener.onSkipInRead(ffpe);

        assertEquals(1, repository.contarRegistrosRechazados());
        List<BatchRegistroRechazado> rechazos = repository.obtenerTodos();
        BatchRegistroRechazado r = rechazos.get(0);

        assertEquals("reporteTransaccionesDiariasJob", r.getJobName());
        assertTrue(r.getStepName().contains("transacciones"));
        assertEquals("LINEA_15", r.getIdentificadorRegistro());
        assertEquals("TX_CORRUPTA,invalid,data", r.getDatosOrigen());
        assertEquals("FlatFileParseException", r.getTipoError());
    }

    @Test
    void testCuentaInteresSkipListener() {
        CuentaInteresDTO dto = new CuentaInteresDTO("", "John Doe", "5000", "30", "ahorro");
        CuentaInteresValidationException ex = new CuentaInteresValidationException("ID de cuenta no puede ser vacío");

        cuentaInteresSkipListener.onSkipInProcess(dto, ex);

        assertEquals(1, repository.contarRegistrosRechazados());
        List<BatchRegistroRechazado> rechazos = repository.obtenerTodos();
        BatchRegistroRechazado r = rechazos.get(0);

        assertEquals("calculoInteresesJob", r.getJobName());
        assertTrue(r.getStepName().contains("intereses"));
        assertEquals("CuentaInteresValidationException", r.getTipoError());
        assertEquals("ID de cuenta no puede ser vacío", r.getMotivoRechazo());
    }

    @Test
    void testCuentaAnualSkipListener() {
        CuentaAnualDTO dto = new CuentaAnualDTO("INV_ID", "2024-01-01", "deposito", "1000", "Prueba");
        CuentaAnualValidationException ex = new CuentaAnualValidationException("ID de cuenta con formato no numérico");

        cuentaAnualSkipListener.onSkipInProcess(dto, ex);

        assertEquals(1, repository.contarRegistrosRechazados());
        List<BatchRegistroRechazado> rechazos = repository.obtenerTodos();
        BatchRegistroRechazado r = rechazos.get(0);

        assertEquals("cuentasAnualesJob", r.getJobName());
        assertTrue(r.getStepName().contains("cuentasAnuales"));
        assertEquals("INV_ID", r.getIdentificadorRegistro());
        assertEquals("CuentaAnualValidationException", r.getTipoError());
        assertEquals("ID de cuenta con formato no numérico", r.getMotivoRechazo());
    }
}
