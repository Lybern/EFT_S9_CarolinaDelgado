package com.bancoxyz.migracion.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración end-to-end para los tres Jobs particionados (Semana 3).
 * Valida la correcta ejecución de PartitionSteps, minionSteps, persistencia en BD y detección de anomalías.
 */
@SpringBootTest
class PartitionedJobsIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("reporteTransaccionesDiariasJob")
    private Job reporteTransaccionesDiariasJob;

    @Autowired
    @Qualifier("calculoInteresesJob")
    private Job calculoInteresesJob;

    @Autowired
    @Qualifier("cuentasAnualesJob")
    private Job cuentasAnualesJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
        jdbcTemplate.update("DELETE FROM intereses_procesados");
        jdbcTemplate.update("DELETE FROM cuentas_anuales_procesadas");
        jdbcTemplate.update("DELETE FROM batch_registros_rechazados");
    }

    @Test
    void testReporteTransaccionesDiariasJobParticionado() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(reporteTransaccionesDiariasJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transacciones_procesadas", Integer.class);
        assertNotNull(count);
        assertEquals(10, count, "Deben haberse insertado los 10 registros procesados en paralelo por las 3 particiones");

        Integer anomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transacciones_procesadas WHERE es_anomala = true", Integer.class);
        assertNotNull(anomCount);
        assertTrue(anomCount >= 2, "Deben haberse detectado anomalías de monto/tipo");
    }

    @Test
    void testCalculoInteresesJobParticionado() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(calculoInteresesJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intereses_procesados", Integer.class);
        assertNotNull(count);
        assertEquals(8, count, "Deben haberse insertado las 8 cuentas procesadas por las 3 particiones");

        // Verificar que se haya calculado interés positivo sobre cuentas de ahorro con saldo positivo
        Integer conInteres = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intereses_procesados WHERE interes_calculado > 0", Integer.class);
        assertNotNull(conInteres);
        assertTrue(conInteres >= 3, "Las cuentas válidas deben tener interés mensual calculado");
    }

    @Test
    void testCuentasAnualesJobParticionado() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(cuentasAnualesJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cuentas_anuales_procesadas", Integer.class);
        assertNotNull(count);
        assertEquals(9, count, "Deben haberse insertado los 9 estados de cuenta anuales");
    }
}
