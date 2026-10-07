package com.bancoxyz.migracion.repository;

import com.bancoxyz.migracion.model.BatchRegistroRechazado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BatchRegistroRechazadoRepositoryTest {

    @Autowired
    private BatchRegistroRechazadoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM batch_registros_rechazados");
    }

    @Test
    void testGuardarYObtenerRegistrosRechazados() {
        BatchRegistroRechazado rechazo = BatchRegistroRechazado.builder()
                .jobName("reporteTransaccionesDiariasJob")
                .stepName("transaccionesStep")
                .identificadorRegistro("TX999")
                .datosOrigen("TransaccionDTO{id='TX999', fecha='invalida', monto='100.00', tipo='DEBITO'}")
                .tipoError("DateTimeParseException")
                .motivoRechazo("Formato de fecha inválido")
                .fechaRegistro(LocalDateTime.now())
                .build();

        repository.guardar(rechazo);

        assertEquals(1, repository.contarRegistrosRechazados());

        List<BatchRegistroRechazado> todos = repository.obtenerTodos();
        assertNotNull(todos);
        assertEquals(1, todos.size());

        BatchRegistroRechazado guardado = todos.get(0);
        assertNotNull(guardado.getId());
        assertEquals("reporteTransaccionesDiariasJob", guardado.getJobName());
        assertEquals("transaccionesStep", guardado.getStepName());
        assertEquals("TX999", guardado.getIdentificadorRegistro());
        assertEquals("DateTimeParseException", guardado.getTipoError());
        assertEquals("Formato de fecha inválido", guardado.getMotivoRechazo());
        assertNotNull(guardado.getFechaRegistro());
    }

    @Test
    void testGuardarMultiplesRegistrosRechazados() {
        BatchRegistroRechazado r1 = BatchRegistroRechazado.builder()
                .jobName("calculoInteresesJob")
                .stepName("interesesStep")
                .identificadorRegistro("CTA_001")
                .datosOrigen("CuentaInteresDTO{cuentaId='CTA_001', saldo='invalid'}")
                .tipoError("NumberFormatException")
                .motivoRechazo("Saldo inválido")
                .build();

        BatchRegistroRechazado r2 = BatchRegistroRechazado.builder()
                .jobName("cuentasAnualesJob")
                .stepName("cuentasAnualesStep")
                .identificadorRegistro("CTA_002")
                .datosOrigen("CuentaAnualDTO{cuentaId='CTA_002', fecha='bad-date'}")
                .tipoError("CuentaAnualValidationException")
                .motivoRechazo("Fecha inválida")
                .build();

        repository.guardar(r1);
        repository.guardar(r2);

        assertEquals(2, repository.contarRegistrosRechazados());
    }
}
