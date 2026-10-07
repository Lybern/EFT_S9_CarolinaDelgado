package com.bancoxyz.migracion.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchScalingBenchmarkTest {

    @Test
    void testEjecutarBenchmarkCompleto() {
        BatchScalingBenchmark benchmark = new BatchScalingBenchmark();
        List<BatchScalingBenchmark.BenchmarkResult> resultados = benchmark.ejecutarComparativa();

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty());
        assertTrue(resultados.size() >= 10);

        // Verificar que la configuración óptima está presente
        boolean optimoPresente = resultados.stream()
                .anyMatch(r -> r.particiones() == 3 && r.hilos() == 3 && r.chunkSize() == 5 && r.eficiencia().toUpperCase().contains("ÓPTIMA"));

        assertTrue(optimoPresente, "El benchmark debe identificar la configuración óptima (3 particiones, 3 hilos, chunk 5)");
    }
}
