package com.bancoxyz.migracion.processor;

import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.exception.CuentaInteresValidationException;
import com.bancoxyz.migracion.model.CuentaInteresProcesada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ItemProcessor para el cálculo de intereses mensuales y validación de cuentas de ahorro y préstamos.
 * Aplica lógica de negocio bancario, tolerancia a fallos y concurrencia segura.
 */
public class CuentaInteresItemProcessor implements ItemProcessor<CuentaInteresDTO, CuentaInteresProcesada> {

    private static final Logger log = LoggerFactory.getLogger(CuentaInteresItemProcessor.class);

    private static final Set<String> TIPOS_VALIDOS = Set.of("AHORRO", "PRESTAMO", "HIPOTECA");
    private static final Set<String> VALORES_INVALIDOS = Set.of("-1", "UNKNOWN", "N/A", "NULL", "DESCONOCIDO", "NONE");

    // Estructura concurrente para seguimiento en múltiples hilos
    private final Set<String> registrosProcesados = ConcurrentHashMap.newKeySet();

    @Override
    public CuentaInteresProcesada process(CuentaInteresDTO item) throws Exception {
        log.debug("[{}] Procesando cuenta para intereses: {}", Thread.currentThread().getName(), item);

        // 1. Validación estricta de Identificador obligatorio
        String cuentaId = item.getCuentaId() != null ? item.getCuentaId().trim() : "";
        if (cuentaId.isBlank()) {
            throw new CuentaInteresValidationException("El ID de la cuenta es obligatorio y no puede ser nulo o vacío.");
        }

        CuentaInteresProcesada procesada = new CuentaInteresProcesada();
        procesada.setCuentaId(cuentaId);

        boolean anomala = false;
        StringBuilder motivos = new StringBuilder();

        // 2. Validar Nombre del Titular
        String nombre = item.getNombre() != null ? item.getNombre().trim() : "";
        if (nombre.isBlank() || VALORES_INVALIDOS.contains(nombre.toUpperCase())) {
            anomala = true;
            motivos.append("Nombre de titular inválido o desconocido (").append(nombre).append("). ");
        }
        procesada.setNombre(nombre.isBlank() ? "DESCONOCIDO" : nombre);

        // 3. Validar Tipo de Cuenta y Normalizar
        String tipoRaw = item.getTipo() != null ? item.getTipo().trim().toUpperCase() : "";
        if (tipoRaw.isBlank() || VALORES_INVALIDOS.contains(tipoRaw) || !TIPOS_VALIDOS.contains(tipoRaw)) {
            anomala = true;
            motivos.append("Tipo de cuenta no estándar o inválido (").append(item.getTipo()).append("). ");
            procesada.setTipo(tipoRaw.isBlank() ? "DESCONOCIDO" : tipoRaw);
        } else {
            procesada.setTipo(tipoRaw);
        }

        // 4. Validar Edad
        if (item.getEdad() == null || item.getEdad().isBlank()) {
            anomala = true;
            motivos.append("Edad no informada. ");
        } else {
            try {
                int edad = Integer.parseInt(item.getEdad().trim());
                if (edad < 18 || edad > 100) {
                    anomala = true;
                    motivos.append("Edad fuera de rango comercial permitido (18-100): ").append(edad).append(". ");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Formato no numérico de edad en cuenta " + cuentaId + ": " + item.getEdad());
            }
        }

        // 5. Validar Saldo
        BigDecimal saldoValido = BigDecimal.ZERO;
        if (item.getSaldo() == null || item.getSaldo().isBlank()) {
            anomala = true;
            motivos.append("Saldo ausente. ");
        } else {
            try {
                saldoValido = new BigDecimal(item.getSaldo().trim());
                if (saldoValido.compareTo(BigDecimal.ZERO) <= 0) {
                    anomala = true;
                    motivos.append("Saldo cero o negativo (").append(saldoValido).append("). ");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Formato de saldo numérico inválido en cuenta " + cuentaId + ": " + item.getSaldo());
            }
        }
        procesada.setSaldo(saldoValido);

        // 6. Detección de Registros Duplicados
        String firmaRegistro = String.format("%s|%s|%s",
                nombre.toLowerCase(),
                saldoValido.toPlainString(),
                procesada.getTipo());

        if (registrosProcesados.contains(firmaRegistro)) {
            anomala = true;
            motivos.append("Registro duplicado detectado. ");
        } else {
            registrosProcesados.add(firmaRegistro);
        }

        // 7. Cálculo de Interés Mensual (Tasa anual 5% -> Tasa mensual = (Saldo * 5%) / 12)
        boolean esTipoValido = TIPOS_VALIDOS.contains(procesada.getTipo());
        if (saldoValido.compareTo(BigDecimal.ZERO) > 0 && esTipoValido) {
            BigDecimal tasaAnual = new BigDecimal("5.00");
            BigDecimal interes = saldoValido
                    .multiply(tasaAnual)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            procesada.setInteresCalculado(interes);
            procesada.setSaldoFinal(saldoValido.add(interes));
        } else {
            procesada.setInteresCalculado(BigDecimal.ZERO);
            procesada.setSaldoFinal(saldoValido);
        }

        // Asignación final de estado y auditoría
        procesada.setEsAnomala(anomala);
        procesada.setMotivoObservacion(anomala ? motivos.toString().trim() : null);

        log.debug("[{}] Cuenta interes procesada id={}: interes={}, saldoFinal={}",
                Thread.currentThread().getName(), procesada.getCuentaId(), procesada.getInteresCalculado(), procesada.getSaldoFinal());
        return procesada;
    }
}