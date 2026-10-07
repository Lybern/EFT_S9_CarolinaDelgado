package com.bancoxyz.migracion.partitioner;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvRangePartitionerTest {

    @Test
    void testParticionar10RegistrosConGridSize3() {
        String csvContent = "id,fecha,monto,tipo\n" +
                "1,2024-01-01,1000,debito\n" +
                "2,2024-01-02,1500,credito\n" +
                "3,2024-01-03,-200,debito\n" +
                "4,2024-01-03,,debito\n" +
                "5,2024/01/04,800,credito\n" +
                "6,2024-01-05,700,debito\n" +
                "7,2024-01-06,1200,credito\n" +
                "8,2024-01-05,700,debito\n" +
                "9,2024-01-07,3000,debito\n" +
                "10,2024-01-08,1000,invalid\n";

        Resource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvRangePartitioner partitioner = new CsvRangePartitioner(resource, 1);

        assertEquals(10, partitioner.countDataRecords());

        Map<String, ExecutionContext> partitions = partitioner.partition(3);
        assertEquals(3, partitions.size());

        // Partición 0: 4 registros (10 / 3 = 3 + 1)
        ExecutionContext p0 = partitions.get("partition0");
        assertNotNull(p0);
        assertEquals(1, p0.getInt("linesToSkip")); // Salta 1 (header)
        assertEquals(4, p0.getInt("maxItemCount"));
        assertEquals(2, p0.getInt("startLine"));
        assertEquals(5, p0.getInt("endLine"));

        // Partición 1: 3 registros (10 / 3 = 3)
        ExecutionContext p1 = partitions.get("partition1");
        assertNotNull(p1);
        assertEquals(5, p1.getInt("linesToSkip")); // Salta 1 + 4
        assertEquals(3, p1.getInt("maxItemCount"));
        assertEquals(6, p1.getInt("startLine"));
        assertEquals(8, p1.getInt("endLine"));

        // Partición 2: 3 registros (10 / 3 = 3)
        ExecutionContext p2 = partitions.get("partition2");
        assertNotNull(p2);
        assertEquals(8, p2.getInt("linesToSkip")); // Salta 5 + 3
        assertEquals(3, p2.getInt("maxItemCount"));
        assertEquals(9, p2.getInt("startLine"));
        assertEquals(11, p2.getInt("endLine"));

        // Suma total de registros = 10
        int sumItems = p0.getInt("maxItemCount") + p1.getInt("maxItemCount") + p2.getInt("maxItemCount");
        assertEquals(10, sumItems);
    }

    @Test
    void testParticionar8RegistrosConGridSize3() {
        String csvContent = "cuenta_id,nombre,saldo,edad,tipo\n" +
                "101,John Doe,5000,30,ahorro\n" +
                "102,Jane Smith,8000,25,prestamo\n" +
                "103,Bob Johnson,12000,,-1\n" +
                "104,Alice Brown,,45,ahorro\n" +
                "105,Charlie Green,7000,35,hipoteca\n" +
                "106,John Doe,5000,30,ahorro\n" +
                "107,Diana Prince,15000,40,prestamo\n" +
                "108,Steve Rogers,10000,100,ahorro\n";

        Resource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvRangePartitioner partitioner = new CsvRangePartitioner(resource, 1);

        assertEquals(8, partitioner.countDataRecords());

        Map<String, ExecutionContext> partitions = partitioner.partition(3);
        assertEquals(3, partitions.size());

        ExecutionContext p0 = partitions.get("partition0");
        ExecutionContext p1 = partitions.get("partition1");
        ExecutionContext p2 = partitions.get("partition2");

        assertEquals(3, p0.getInt("maxItemCount"));
        assertEquals(3, p1.getInt("maxItemCount"));
        assertEquals(2, p2.getInt("maxItemCount"));

        int total = p0.getInt("maxItemCount") + p1.getInt("maxItemCount") + p2.getInt("maxItemCount");
        assertEquals(8, total);
    }

    @Test
    void testParticionarArchivoVacio() {
        String csvContent = "header1,header2\n";
        Resource resource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvRangePartitioner partitioner = new CsvRangePartitioner(resource, 1);

        assertEquals(0, partitioner.countDataRecords());

        Map<String, ExecutionContext> partitions = partitioner.partition(3);
        assertEquals(1, partitions.size());
        assertEquals(0, partitions.get("partition0").getInt("maxItemCount"));
    }
}
