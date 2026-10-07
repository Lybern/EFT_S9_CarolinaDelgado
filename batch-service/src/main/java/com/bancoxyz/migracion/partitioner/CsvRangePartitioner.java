package com.bancoxyz.migracion.partitioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Particionador de archivos CSV para Spring Batch (Semana 3).
 * Divide los registros del archivo plano en particiones equilibradas y disjuntas,
 * calculando para cada partición el desplazamiento de líneas (linesToSkip) y la
 * cantidad máxima de registros a procesar (maxItemCount).
 */
public class CsvRangePartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(CsvRangePartitioner.class);

    private final Resource resource;
    private final int headerLines;

    public CsvRangePartitioner(Resource resource) {
        this(resource, 1);
    }

    public CsvRangePartitioner(Resource resource, int headerLines) {
        this.resource = resource;
        this.headerLines = Math.max(0, headerLines);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        int totalRecords = countDataRecords();
        log.info("Particionando recurso [{}] con {} registros de datos y gridSize={}",
                resource != null ? resource.getFilename() : "null", totalRecords, gridSize);

        if (totalRecords == 0 || gridSize <= 0) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("partitionNumber", 0);
            context.putString("partitionName", "partition0");
            context.putInt("linesToSkip", headerLines);
            context.putInt("maxItemCount", 0);
            context.putInt("startLine", headerLines + 1);
            context.putInt("endLine", headerLines);
            context.putInt("totalRecords", 0);
            result.put("partition0", context);
            return result;
        }

        int actualPartitions = Math.min(gridSize, totalRecords);
        int baseSize = totalRecords / actualPartitions;
        int remainder = totalRecords % actualPartitions;

        int currentLineOffset = headerLines; // Líneas a saltar para llegar al primer registro de la partición

        for (int i = 0; i < actualPartitions; i++) {
            int countForPartition = baseSize + (i < remainder ? 1 : 0);
            int startLine = currentLineOffset + 1;
            int endLine = currentLineOffset + countForPartition;

            ExecutionContext context = new ExecutionContext();
            context.putInt("partitionNumber", i);
            context.putString("partitionName", "partition" + i);
            context.putInt("linesToSkip", currentLineOffset);
            context.putInt("maxItemCount", countForPartition);
            context.putInt("startLine", startLine);
            context.putInt("endLine", endLine);
            context.putInt("totalRecords", totalRecords);

            String partitionKey = "partition" + i;
            result.put(partitionKey, context);

            log.info("  -> [Particionador] {} configurada: linesToSkip={}, maxItemCount={}, lineas {}-{} ({} registros)",
                    partitionKey, currentLineOffset, countForPartition, startLine, endLine, countForPartition);

            currentLineOffset += countForPartition;
        }

        return result;
    }

    /**
     * Cuenta la cantidad de registros de datos en el archivo CSV descartando las líneas de encabezado.
     */
    public int countDataRecords() {
        if (resource == null || !resource.exists()) {
            return 0;
        }
        int totalLines = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                totalLines++;
            }
        } catch (Exception e) {
            log.error("Error al contar líneas en recurso {}: {}", resource.getFilename(), e.getMessage());
            return 0;
        }
        return Math.max(0, totalLines - headerLines);
    }
}
