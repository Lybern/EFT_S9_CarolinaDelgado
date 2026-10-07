package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.exception.CuentaAnualValidationException;
import com.bancoxyz.migracion.model.CuentaAnualProcesada;
import com.bancoxyz.migracion.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ItemProcessor para la compilación y validación de estados de cuenta anuales
 * para reportes de auditoría bancaria.
 * Implementa concurrencia segura, detección previa de duplicados y
 * utiliza DateUtil para la validación y parseo centralizado de fechas.
 */
@Component
public class CuentaAnualItemProcessor implements ItemProcessor<CuentaAnualDTO, CuentaAnualProcesada> {

    private static final Logger log = LoggerFactory.getLogger(CuentaAnualItemProcessor.class);

    // Estructura thread-safe para multithreading y detección previa de duplicados
    private final Set<String> registrosProcesados = ConcurrentHashMap.newKeySet();

    @Override
    public CuentaAnualProcesada process(CuentaAnualDTO item) throws Exception {
        log.debug("[{}] Procesando estado de cuenta anual: {}", Thread.currentThread().getName(), item);
        List<String> motivos = new ArrayList<>();

        // 1. Validar Identificador de Cuenta (Numérico obligatorio)
        Long cuentaId;
        if (item.getCuentaId() == null || item.getCuentaId().isBlank()) {
            throw new CuentaAnualValidationException("El ID de cuenta es obligatorio y no puede ser nulo o vacío.");
        }
        try {
            cuentaId = Long.parseLong(item.getCuentaId().trim());
        } catch (NumberFormatException e) {
            throw new CuentaAnualValidationException("Identificador de cuenta con formato no numérico: " + item.getCuentaId());
        }

        // 2. Parseo y Validación de Fecha centralizada con DateUtil
        LocalDate fechaResultante = DateUtil.parsearFecha(item.getFecha());
        if (fechaResultante == null) {
            if (item.getFecha() != null && !item.getFecha().isBlank()) {
                throw new CuentaAnualValidationException("Fecha con formato inválido o no reconocido: " + item.getFecha());
            } else {
                motivos.add("Fecha faltante.");
            }
        }

        // 3. Normalización y Validación de Monto
        BigDecimal montoResultante = BigDecimal.ZERO;
        if (item.getMonto() != null && !item.getMonto().isBlank()) {
            try {
                montoResultante = new BigDecimal(item.getMonto().trim());
                if (montoResultante.compareTo(BigDecimal.ZERO) < 0) {
                    motivos.add("Monto de transacción negativo (" + montoResultante + ").");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Monto con formato numérico inválido: " + item.getMonto());
            }
        } else {
            motivos.add("Monto faltante.");
        }

        // 4. Normalización de Descripción
        String descripcionResultante = item.getDescripcion();
        if (descripcionResultante == null || descripcionResultante.isBlank()) {
            motivos.add("Descripción ausente.");
            descripcionResultante = "SIN_DESCRIPCION";
        } else {
            descripcionResultante = descripcionResultante.trim();
        }

        String transaccionTipo = item.getTransaccion() != null ? item.getTransaccion().trim().toUpperCase() : "DESCONOCIDO";

        // 5. Detección de Registros Duplicados (firma multithread)
        String firmaRegistro = String.format("%s|%s|%s|%s|%s",
                cuentaId,
                fechaResultante != null ? fechaResultante.toString() : "SIN_FECHA",
                transaccionTipo,
                montoResultante.toPlainString(),
                descripcionResultante);

        if (registrosProcesados.contains(firmaRegistro)) {
            motivos.add("Registro duplicado detectado.");
        } else {
            registrosProcesados.add(firmaRegistro);
        }

        // 6. Evaluación de Anomalías
        boolean esAnomala = !motivos.isEmpty();
        String motivoObservacion = esAnomala ? String.join(" ", motivos) : null;

        log.debug("[{}] Cuenta anual procesada cuentaId={}: esAnomala={}", Thread.currentThread().getName(), cuentaId, esAnomala);

        return CuentaAnualProcesada.builder()
                .cuentaId(cuentaId)
                .fecha(fechaResultante)
                .transaccion(transaccionTipo)
                .monto(montoResultante)
                .descripcion(descripcionResultante)
                .esAnomala(esAnomala)
                .motivoObservacion(motivoObservacion)
                .build();
    }
}