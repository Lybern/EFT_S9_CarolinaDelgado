# Proyecto Spring Batch: Migración y Procesamiento Bancario Escalable (Semana 3)

Microservicio batch desarrollado en **Spring Boot 3.3.2**, **Spring Batch 5** y **Java 17** para el **Banco XYZ**. Procesa masivamente información financiera aplicando **escalado mediante particiones distribuidas (Manager / Worker Steps)**, validaciones estrictas de negocio, detección de anomalías, tolerancia a fallos y persistencia en base de datos relacional.

---

## Jobs Configurados y Particionados

La aplicación ejecuta de forma secuencial tres Jobs particionados mediante `BatchJobRunner`:

1. **`reporteTransaccionesDiariasJob`**: 
   - Step Maestro: `transaccionesPartitionStep` (divide el CSV en 3 particiones con `CsvRangePartitioner`).
   - Step Worker: `transaccionesMinionStep` (ejecutado concurrentemente por `TaskExecutorPartitionHandler` en `ThreadPoolTaskExecutor`).
   - Procesa transacciones, detecta montos negativos, duplicados o tipos inválidos, y persiste en `transacciones_procesadas`.

2. **`calculoInteresesJob`**: 
   - Step Maestro: `interesesPartitionStep`.
   - Step Worker: `interesesMinionStep`.
   - Evalúa saldos de cuentas de ahorro y préstamos, valida titulares y rangos de edad (18-100), calcula intereses mensuales con tasa del 5% anual (`(saldo * 5%) / 12`) y persiste en `intereses_procesados`.

3. **`cuentasAnualesJob`**: 
   - Step Maestro: `cuentasAnualesPartitionStep`.
   - Step Worker: `cuentasAnualesMinionStep`.
   - Compila estados de cuenta anuales, normaliza descripciones y fechas con `DateUtil` y persiste en `cuentas_anuales_procesadas`.

---

## Tolerancia a Fallos, Reintentos y Auditoría

* **`FileVerificationSkipper`**: Política `SkipPolicy` con límite de 10 omisiones ante excepciones de formato (`FlatFileParseException`, `DateTimeParseException`, `NumberFormatException`, validaciones de negocio).
* **`AbstractBatchSkipListener` / `SkipListener`**: Auditoría dual en tabla `batch_registros_rechazados` y en archivo `data/errores.csv`.
* **Reintentos**: `retryLimit(3)` ante excepciones transitorias de BD (`CannotAcquireLockException`, `DeadlockLoserDataAccessException`, `TransientDataAccessException`).

---

## Detección y Normalización de Anomalías

Para garantizar la trazabilidad bancaria y cuadre contable:
* **Valores Inválidos / Desconocidos**: Se normalizan a `"DESCONOCIDO"`, se marca `es_anomala = true` y se registra el motivo de observación sin descartar la fila.
* **Duplicados**: Se detectan mediante firma thread-safe en el `ItemProcessor`, marcando `es_anomala = true` y `"Registro duplicado detectado."`
* **Auditoría**: Ningún registro válido o anómalo se pierde silenciosamente, asegurando consistencia frente a auditorías financieras.
