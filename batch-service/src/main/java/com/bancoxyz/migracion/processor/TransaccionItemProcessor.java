package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.exception.TransaccionValidationException;
import com.bancoxyz.migracion.model.TransaccionProcesada;
import com.bancoxyz.migracion.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ItemProcessor para la normalización, validación y detección de anomalías
 * en las transacciones diarias del Banco XYZ.
 * Implementa concurrencia segura y manejo de excepciones de validación.
 * Utiliza DateUtil para la validación y parseo centralizado de fechas.
 */
public class TransaccionItemProcessor implements ItemProcessor<TransaccionDTO, TransaccionProcesada> {

    private static final Logger log = LoggerFactory.getLogger(TransaccionItemProcessor.class);

    private static final Set<String> TIPOS_VALIDOS = Set.of("DEBITO", "CREDITO");

    // Estructura thread-safe para multithreading
    private final Set<String> registrosProcesados = ConcurrentHashMap.newKeySet();

    @Override
    public TransaccionProcesada process(TransaccionDTO item) throws Exception {
        log.debug("[{}] Procesando transacción: {}", Thread.currentThread().getName(), item);

        // 1. Validación estricta de Identificador obligatorio (si falta, se lanza excepción para skip)
        if (item.getId() == null || item.getId().isBlank()) {
            throw new TransaccionValidationException("El ID de la transacción es obligatorio y no puede ser nulo o vacío.");
        }

        TransaccionProcesada procesada = new TransaccionProcesada();
        procesada.setTransaccionId(item.getId().trim());

        boolean anomala = false;
        StringBuilder motivos = new StringBuilder();

        // 2. Normalización y Parseo de Fecha centralizado con DateUtil
        LocalDate fechaParsed = DateUtil.parsearFecha(item.getFecha());
        if (fechaParsed == null) {
            // Si la fecha es un valor corrupto no parseable, lanzar TransaccionValidationException para skip
            if (item.getFecha() != null && !item.getFecha().isBlank()) {
                throw new TransaccionValidationException("Fecha inválida con formato no reconocible: " + item.getFecha());
            } else {
                anomala = true;
                motivos.append("Fecha ausente. ");
            }
        }
        procesada.setFechaTransaccion(fechaParsed);

        // 3. Conversión y Validación de Monto
        BigDecimal montoDecimal = BigDecimal.ZERO;
        if (item.getMonto() != null && !item.getMonto().isBlank()) {
            try {
                montoDecimal = new BigDecimal(item.getMonto().trim());
                procesada.setMonto(montoDecimal);

                if (montoDecimal.compareTo(BigDecimal.ZERO) < 0) {
                    anomala = true;
                    motivos.append("Monto negativo (").append(montoDecimal).append("). ");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Formato numérico de monto inválido en transacción " + item.getId() + ": " + item.getMonto());
            }
        } else {
            anomala = true;
            motivos.append("Monto faltante. ");
            procesada.setMonto(BigDecimal.ZERO);
        }

        // 4. Validación de Tipo de Transacción
        String tipoLimpio = item.getTipo() != null ? item.getTipo().trim().toUpperCase() : "";
        if (!TIPOS_VALIDOS.contains(tipoLimpio)) {
            anomala = true;
            motivos.append("Tipo de transacción no estándar (").append(item.getTipo()).append("). ");
        }
        procesada.setTipoTransaccion(tipoLimpio.isBlank() ? "DESCONOCIDO" : tipoLimpio);

        // 5. Detección de Registros Duplicados (firma multithread)
        String firmaRegistro = String.format("%s|%s|%s|%s",
                procesada.getTransaccionId(),
                fechaParsed != null ? fechaParsed.toString() : "SIN_FECHA",
                montoDecimal.toPlainString(),
                tipoLimpio);

        if (registrosProcesados.contains(firmaRegistro)) {
            anomala = true;
            motivos.append("Registro duplicado detectado. ");
        } else {
            registrosProcesados.add(firmaRegistro);
        }

        // 6. Asignación final de estado
        procesada.setEsAnomala(anomala);
        procesada.setMotivoObservacion(anomala ? motivos.toString().trim() : null);

        log.debug("[{}] Transacción procesada id={}: esAnomala={}", Thread.currentThread().getName(), procesada.getTransaccionId(), anomala);
        return procesada;
    }
}