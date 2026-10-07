# Sistema de Migración de Procesos Batch - Banco XYZ (Semana 3)

Proyecto de modernización y optimización de procesos batch financieros desarrollado con **Spring Boot 3.3.2**, **Spring Batch 5** y **Java 17** para el **Banco XYZ**, correspondiente a la **Semana 3 (Exp 1 - Semana 3: "Optimizando procesos batch para mejorar la resiliencia de procesos")**.

Esta versión incorpora **escalado mediante particiones distribuidas (`PartitionStep` y `minionSteps` en paralelo)**, **manejador de particiones `TaskExecutorPartitionHandler` (gridSize=3)**, **lectores particionados aislados con `@StepScope`**, **políticas personalizadas de tolerancia a fallos (`FileVerificationSkipper`)**, **reintentos automáticos ante bloqueos de base de datos (`RetryPolicy`)**, **auditoría integral en tabla Dead-Letter de base de datos (`batch_registros_rechazados`) y en archivo CSV (`data/errores.csv`)**, y un **módulo de benchmarking comparativo de parámetros** para justificar la configuración óptima del sistema.

---

## 1. Objetivos del Proyecto

El objetivo principal es migrar y optimizar los sistemas legacy del Banco XYZ mediante Spring Batch, garantizando máxima resiliencia, integridad de datos y alta eficiencia concurrente en tres procesos clave:

1. **Reporte de Transacciones Diarias (`reporteTransaccionesDiariasJob`)**: 
   - Procesa transacciones financieras del día distribuidas en 3 particiones concurrentes.
   - Detecta y clasifica anomalías (montos negativos, tipos de operación no estándar, duplicados, campos ausentes).
   - Registra transacciones normalizadas en la tabla relacional `transacciones_procesadas`.
2. **Cálculo de Intereses Mensuales (`calculoInteresesJob`)**: 
   - Evalúa saldos de cuentas de ahorro y préstamos particionadas en paralelo.
   - Valida coherencia de titulares, tipos de cuenta y rangos etarios comerciales (18 a 100 años).
   - Aplica la fórmula bancaria de interés mensual sobre tasa anual del 5%:  
     $$\text{Interés Mensual} = \frac{\text{Saldo} \times 5\%}{12}$$
   - Actualiza el saldo final persistiendo en `intereses_procesados`.
3. **Generación de Estados de Cuenta Anuales (`cuentasAnualesJob`)**: 
   - Compila y valida movimientos anuales de cuentas de clientes bancarios.
   - Normaliza transacciones, montos y descripciones para informes de auditoría financiera.
   - Persiste los estados de cuenta en `cuentas_anuales_procesadas`.

---

## 3. Arquitectura del Sistema Batch con Particiones

### Flujo de Particionamiento Maestro / Esclavo (Manager - Worker)

```
                            ┌─────────────────────────────────────────────────┐
                            │               Job Principal Batch               │
                            └────────────────────────┬────────────────────────┘
                                                     │
                                                     ▼
                            ┌─────────────────────────────────────────────────┐
                            │          PartitionStep (Step Maestro)           │
                            │   Divide el CSV con CsvRangePartitioner         │
                            └────────────────────────┬────────────────────────┘
                                                     │
                                                     ▼
                            ┌─────────────────────────────────────────────────┐
                            │    TaskExecutorPartitionHandler (gridSize = 3)  │
                            │         Distribuye a ThreadPoolTaskExecutor     │
                            └───────┬────────────────┬────────────────┬───────┘
                                    │                │                │
                    [ Batch-Thread-1 ]        [ Batch-Thread-2 ]      [ Batch-Thread-3 ]
                            │                        │                        │
                            ▼                        ▼                        ▼
               ┌────────────────────────┐┌────────────────────────┐┌────────────────────────┐
               │ minionStep:partition0  ││ minionStep:partition1  ││ minionStep:partition2  │
               │ (Registros 1 al 4)     ││ (Registros 5 al 7)     ││ (Registros 8 al 10)    │
               │                        ││                        ││                        │
               │ • @StepScope Reader    ││ • @StepScope Reader    ││ • @StepScope Reader    │
               │ • ItemProcessor        ││ • ItemProcessor        ││ • ItemProcessor        │
               │ • JdbcBatchItemWriter  ││ • JdbcBatchItemWriter  ││ • JdbcBatchItemWriter  │
               │ • Skip/Retry Policies  ││ • Skip/Retry Policies  ││ • Skip/Retry Policies  │
               └────────────┬───────────┘└────────────┬───────────┘└────────────┬───────────┘
                            │                        │                        │
                            └────────────────────────┼────────────────────────┘
                                                     │
                                                     ▼
                            ┌─────────────────────────────────────────────────┐
                            │               Join & Aggregate                  │
                            │    Consolida métricas y estado del Job          │
                            └─────────────────────────────────────────────────┘
```

### Diferencias Clave entre Arquitecturas

| Aspecto | Secuencial (Semana 1) | Multihilo Simple (Semana 2) | Particiones (Semana 3 - Implementada) |
| :--- | :--- | :--- | :--- |
| **Modelo de Concurrencia** | Mono-hilo (`[main]`). | Hilos paralelos consumiendo un único Reader compartido. | Partición disjunta asignada a pasos esclavos independientes (`minionStep`). |
| **Seguridad de Lectura** | Segura pero lenta. | Riesgo de sincronización en stream de lectura (`ChunkMonitor warning`). | **100% Aislada y thread-safe** mediante `@StepScope` en cada partición. |
| **Aislamiento de Errores** | El fallo detiene todo el proceso. | El fallo puede afectar el chunk concurrente. | **Aislado**: El reintento o skip ocurre solo en la partición afectada. |
| **Tiempos de Ejecución** | ~329 ms (Base). | ~245 ms (-25.5%). | **~168 ms (-48.9% de reducción)**. |

---

## 4. Estructura del Código Fuente

```text
src/
├── main/
│   ├── java/com/bancoxyz/migracion/
│   │   ├── MigracionApplication.java         # Clase de arranque Spring Boot con salida limpia
│   │   ├── BatchJobRunner.java               # Runner que orquesta los 3 jobs, auditoría y benchmarks
│   │   ├── benchmark/                        # Módulo de Benchmarking y Comparativa
│   │   │   └── BatchScalingBenchmark.java    # Matriz comparativa de parámetros y escalamiento
│   │   ├── config/                           # Configuraciones de Infraestructura y Jobs
│   │   │   ├── BatchInfrastructureConfig.java# ThreadPoolTaskExecutor (3 hilos, pool de 10)
│   │   │   ├── TransaccionesJobConfig.java   # Job 1: Transacciones Diarias (Particionado)
│   │   │   ├── InteresesJobConfig.java       # Job 2: Cálculo de Intereses (Particionado)
│   │   │   └── CuentasAnualesJobConfig.java  # Job 3: Estados de Cuenta Anuales (Particionado)
│   │   ├── dto/                              # DTOs de entrada leídos desde CSVs
│   │   │   ├── TransaccionDTO.java
│   │   │   ├── CuentaInteresDTO.java
│   │   │   └── CuentaAnualDTO.java
│   │   ├── exception/                        # Jerarquía de Excepciones de Validación
│   │   │   ├── BatchValidationException.java
│   │   │   ├── TransaccionValidationException.java
│   │   │   ├── CuentaInteresValidationException.java
│   │   │   ├── CuentaAnualValidationException.java
│   │   │   └── InvalidDataException.java
│   │   ├── listener/                         # Listeners de Ciclo de Vida y Auditoría
│   │   │   ├── AbstractBatchSkipListener.java# Persistencia en batch_registros_rechazados y errores.csv
│   │   │   ├── BatchJobCompletionListener.java
│   │   │   ├── BatchStepExecutionListener.java
│   │   │   ├── TransaccionSkipListener.java
│   │   │   ├── CuentaInteresSkipListener.java
│   │   │   ├── CuentaAnualSkipListener.java
│   │   │   ├── BatchSkipListener.java
│   │   │   └── ExecutorShutdown.java         # Graceful shutdown del pool de hilos vía @PreDestroy
│   │   ├── model/                            # Modelos de entidad persistidos en Base de Datos
│   │   │   ├── BatchRegistroRechazado.java   # Entidad Dead-Letter / Auditoría
│   │   │   ├── TransaccionProcesada.java
│   │   │   ├── CuentaInteresProcesada.java
│   │   │   └── CuentaAnualProcesada.java
│   │   ├── partitioner/                      # Particionadores de Spring Batch
│   │   │   └── CsvRangePartitioner.java      # Particionador dinámico de líneas CSV
│   │   ├── policy/                           # Políticas Personalizadas de Tolerancia a Fallos
│   │   │   └── FileVerificationSkipper.java  # Custom SkipPolicy con límite de 10 omisiones
│   │   ├── processor/                        # Lógica de Negocio, Normalización y Validación
│   │   │   ├── TransaccionItemProcessor.java
│   │   │   ├── CuentaInteresItemProcessor.java
│   │   │   └── CuentaAnualItemProcessor.java
│   │   ├── repository/                       # Repositorio JDBC de Rechazos
│   │   │   └── BatchRegistroRechazadoRepository.java
│   │   └── util/                             # Utilidades Centralizadas
│   │       └── DateUtil.java                 # Validador y parser multiformato estricto de fechas
│   └── resources/
│       ├── application.properties            # Propiedades de configuración (MySQL / H2)
│       ├── schema.sql                        # DDL de tablas procesadas y auditoría
│       └── data/                             # Archivos CSV de origen
│           ├── transacciones.csv
│           ├── intereses.csv
│           └── cuentas_anuales.csv
└── test/
    └── java/com/bancoxyz/migracion/
        ├── MigracionApplicationTests.java
        ├── benchmark/
        │   └── BatchScalingBenchmarkTest.java
        ├── integration/
        │   └── PartitionedJobsIntegrationTest.java # Pruebas E2E de Jobs particionados
        ├── listener/
        │   └── SkipListenerIntegrationTest.java
        ├── partitioner/
        │   └── CsvRangePartitionerTest.java   # Pruebas de rangos y particionado
        ├── policy/
        │   └── FileVerificationSkipperTest.java
        ├── processor/
        │   ├── TransaccionItemProcessorTest.java
        │   ├── CuentaInteresItemProcessorTest.java
        │   └── CuentaAnualItemProcessorTest.java
        ├── repository/
        │   └── BatchRegistroRechazadoRepositoryTest.java
        └── util/
            └── DateUtilTest.java
```

---

## 5. Estudio Comparativo de Parámetros y Benchmarking (Criterio 4)

Para cumplir con el **Criterio 4** ("comparando diferentes parámetros para encontrar la configuración óptima"), se evaluaron cuatro dimensiones clave:

### 1. Comparativa de Arquitecturas Batch

| Arquitectura | Particiones | Hilos | Chunk Size | Tiempo (ms) | Throughput (reg/s) | Evaluación |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Secuencial Lineal (Baseline)** | 1 | 1 | 5 | 329 ms | 82.0 reg/s | Baja: Bloqueo de I/O y CPU mono-hilo. |
| **Multihilo en Chunks** | 1 | 3 | 5 | 245 ms | 110.2 reg/s | Media: Contención en el `FlatFileItemReader`. |
| **Particiones (Manager / Worker)** | **3** | **3** | **5** | **168 ms** | **160.7 reg/s** | **Óptima**: Reducción del 48.9% en tiempo; stream 100% aislado. |

### 2. Variación de Grid Size (Cantidad de Particiones)

| Grid Size | Hilos | Chunk | Tiempo (ms) | Eficiencia | Observación Técnica |
| :---: | :---: | :---: | :---: | :--- | :--- |
| **1 Partición** | 3 | 5 | 252 ms | Media-Baja | Desaprovecha el pool de threads disponibles. |
| **2 Particiones** | 3 | 5 | 195 ms | Buena | Subutiliza 1 hilo del pool configurado. |
| **3 Particiones (ÓPTIMO)** | **3** | **5** | **168 ms** | **ÓPTIMA** | **Balance 1:1 entre particiones de datos y threads core.** |
| **5 Particiones** | 3 | 5 | 182 ms | Media | Context-switching y encolamiento innecesario. |
| **8 Particiones** | 3 | 5 | 210 ms | Baja | Sobrecarga de metadata en particiones diminutas. |

### 3. Variación de Pool de Hilos (ThreadPool Core Size)

| Core Threads | Particiones | Chunk | Tiempo (ms) | Eficiencia | Observación Técnica |
| :---: | :---: | :---: | :---: | :--- | :--- |
| **1 Hilo** | 3 | 5 | 290 ms | Baja | Ejecución serializada de particiones en cola. |
| **3 Hilos (ÓPTIMO)** | **3** | **5** | **168 ms** | **ÓPTIMA** | **Máxima concurrencia sin sobrecargar CPU ni conexiones JDBC.** |
| **6 Hilos** | 3 | 5 | 175 ms | Buena | 3 hilos ociosos; consumo extra de descriptores. |
| **10 Hilos** | 3 | 5 | 185 ms | Regular | Sobrecarga de gestión del pool para la carga actual. |

### 4. Variación de Tamaño de Chunk (Chunk Size)

| Chunk Size | Particiones | Hilos | Tiempo (ms) | Eficiencia | Observación Técnica |
| :---: | :---: | :---: | :---: | :--- | :--- |
| **2 Registros** | 3 | 3 | 215 ms | Media-Baja | Excesivos commits y transacciones JDBC. |
| **5 Registros (ÓPTIMO)** | **3** | **3** | **168 ms** | **ÓPTIMA** | **Compromiso ideal entre tamaño de memoria y costo transaccional.** |
| **10 Registros** | 3 | 3 | 180 ms | Buena | Menos commits, pero mayor latencia en reintentos ante fallos. |
| **20 Registros** | 3 | 3 | 205 ms | Regular | Riesgo de rollback masivo ante errores esporádicos. |

> **Justificación de la Configuración Seleccionada:**  
> La configuración **`GridSize = 3`, `CorePoolSize = 3`, `ChunkSize = 5`, `SkipLimit = 10`, `RetryLimit = 3`** es la óptima porque alinea de manera exacta la cantidad de particiones con la capacidad del procesador, minimiza los tiempos muertos de I/O, asegura transacciones acotadas y elimina advertencias de concurrencia.

---

## 6. Políticas de Tolerancia a Fallos y Reintentos (Criterio 5)

### 1. Política Personalizada de Salto (`FileVerificationSkipper`)
Implementa la interfaz `SkipPolicy` de Spring Batch:
* **Límite de omisiones (`skipLimit = 10`)**: Si el número de errores en un Step supera este valor, se lanza `SkipLimitExceededException` para proteger la base de datos de contaminación masiva.
* **Excepciones Omitibles Controladas**:
  - `FlatFileParseException`: Líneas corruptas o mal delimitadas en CSV.
  - `DateTimeParseException`: Fechas en formatos no bancarios o corruptos.
  - `NumberFormatException`: Montos o identificadores con caracteres alfanuméricos no válidos.
  - `TransaccionValidationException`, `CuentaInteresValidationException`, `CuentaAnualValidationException`: Validaciones críticas de negocio (IDs ausentes, cuentas vacías).
  - `InvalidDataException`, `BatchValidationException`, `IllegalArgumentException`.

### 2. Política de Reintentos Automáticos (`RetryPolicy`)
* **Límite de reintentos (`retryLimit = 3`)**:
  - `CannotAcquireLockException`: Reintenta si hay bloqueos de fila concurrentes en MySQL.
  - `DeadlockLoserDataAccessException`: Reintenta ante interbloqueos temporales en transacciones batch paralelas.
  - `TransientDataAccessException`: Reintenta ante fallos transitorios de conectividad o red.

### 3. Sistema de Auditoría Dual (Dead Letter Table + CSV)
* **Tabla `batch_registros_rechazados` en Base de Datos**:
  - Guarda: `id`, `job_name`, `step_name`, `identificador_registro`, `datos_origen`, `tipo_error`, `motivo_rechazo`, `fecha_registro`.
* **Archivo `data/errores.csv`**:
  - Registro de auditoría para auditorías externas y equipos de operaciones.

---

## 7. Estrategia de Idempotencia y Detección de Duplicados (Retroalimentación Semana 2)

En respuesta a la retroalimentación docente (*"Incorporar idempotencia y detección previa de duplicados para prevenir persistencias repetidas"*), se ha formalizado e implementado una estrategia integral de resiliencia y consistencia de datos:

### 1. Definición de Identificación Única (Firma / Clave Natural)
Cada entidad procesada cuenta con una identificación única definida a partir de su clave de negocio o una firma compuesta determinista:
* **Transacciones (`transacciones_procesadas`)**: Identificador natural `transaccion_id` y firma compuesta `[transaccion_id|fecha|monto|tipo]`.
* **Cálculo de Intereses (`intereses_procesados`)**: Identificador natural `cuenta_id` y firma de perfil financiero `[nombre|saldo|tipo]`.
* **Estados de Cuenta Anuales (`cuentas_anuales_procesadas`)**: Identificador `cuenta_id` y firma compuesta de movimiento `[cuenta_id|fecha|transaccion|monto|descripcion]`.

### 2. Criterios de Distinción entre Escenarios de Procesamiento
Para asegurar la coherencia contable y la auditabilidad del sistema, se distinguen claramente tres situaciones operativas:

```
                                  ┌──────────────────────────────────────────────┐
                                  │           Registro Leído del CSV             │
                                  └──────────────────────┬───────────────────────┘
                                                         │
                                  ┌──────────────────────▼───────────────────────┐
                                  │ ¿Formato corrupto o Identificador ausente?   │
                                  └──────────────┬───────────────────────────────┘
                                                 │
                                ┌────────────────┴────────────────┐
                             SÍ │                                 │ NO
                                ▼                                 ▼
                 ┌─────────────────────────────┐   ┌─────────────────────────────┐
                 │       SkipPolicy            │   │  ItemProcessor Detección    │
                 │ (FileVerificationSkipper)   │   │     de Duplicados           │
                 └──────────────┬──────────────┘   └──────────────┬──────────────┘
                                │                                 │
                                ▼                                 │
                 ┌─────────────────────────────┐                  │
                 │  Auditoría en Dead-Letter   │                  │
                 │  - batch_registros_         │                  │
                 │    rechazados               │                  │
                 │  - data/errores.csv         │                  │
                 └─────────────────────────────┘                  │
                                                                  │
              ┌───────────────────────────────────────────────────┼───────────────────────────────────────────────────┐
              │                                                   │                                                   │
              ▼                                                   ▼                                                   ▼
┌───────────────────────────┐                       ┌───────────────────────────┐                       ┌───────────────────────────┐
│     1. CARGA NUEVA        │                       │   2. DUPLICADO EN ORIGEN  │                       │  3. REEJECUCIÓN DEL JOB   │
├───────────────────────────┤                       ├───────────────────────────┤                       ├───────────────────────────┤
│ • Primera vez detectado.  │                       │ • Firma repetida en CSV.  │                       │ • Reinicio tras caída.    │
│ • es_anomala = FALSE      │                       │ • es_anomala = TRUE       │                       │ • Metadatos en Spring     │
│ • Persiste en BD destino. │                       │ • motivo = 'Registro      │                       │   Batch Repositories.     │
│ • Transacción exitosa.    │                       │   duplicado detectado.'   │                       │ • Chunks transaccionales  │
│                           │                       │ • Persiste para auditoría │                       │   atómicos (rollback +    │
│                           │                       │   sin alterar balances.   │                       │   reintento retryLimit=3).│
└───────────────────────────┘                       └───────────────────────────┘                       └───────────────────────────┘
```

1. **Carga Nueva (New Load)**:
   - **Criterio**: Registro con clave/firma no observada previamente en la memoria de trabajo del lote ni en la base de datos.
   - **Tratamiento**: Se aplican las transformaciones y validaciones de negocio normales, se calcula el balance/interés correspondiente y se persiste con `es_anomala = FALSE`.
2. **Repetición en Archivo de Origen (Duplicate in Source)**:
   - **Criterio**: El registro aparece más de una vez dentro del mismo archivo CSV fuente durante la misma ventana de procesamiento.
   - **Tratamiento**: El `ItemProcessor` intercepta la firma repetida mediante un conjunto concurrente thread-safe (`ConcurrentHashMap.newKeySet()`). En lugar de descartar la fila silenciosamente, se marca `es_anomala = TRUE` con el motivo `"Registro duplicado detectado."` y se persiste en la tabla destino. Esto preserva la trazabilidad para auditorías externas sin que los cálculos financieros se dupliquen.
3. **Reejecución del Mismo Trabajo (Job Re-run / Idempotencia de Proceso)**:
   - **Criterio**: El proceso batch es reiniciado o reanudado tras una interrupción imprevista de red, caída de nodo o bloqueo de base de datos.
   - **Tratamiento**: Spring Batch mantiene el estado de ejecución y puntos de control en su esquema de metadatos (`BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`). Las escrituras en chunks de 5 registros están encapsuladas en transacciones ACID con `PlatformTransactionManager`, de modo que si ocurre un fallo, se realiza rollback del chunk completo y se reintenta hasta 3 veces (`retryLimit=3`). Para ejecuciones completas, los parámetros identificadores (`JobParameters`) previenen la ejecución duplicada no intencionada del mismo `JobInstance`.

### 3. Registro y Evidencia en Auditoría y Trazabilidad
* **Tabla `batch_registros_rechazados` (Dead-Letter Table)**: Almacena exclusivamente omisiones por excepciones críticas irrecuperables (campos obligatorios nulos, sintaxis corrupta).
* **Tablas de Negocio (`*_procesadas`)**: Contienen todos los registros procesados con columnas `es_anomala` y `motivo_observacion`, permitiendo filtrar y auditar anomalías y duplicados en cualquier momento mediante consultas SQL estándar.
* **Métricas y Trazas**: El componente `BatchJobRunner` y los listeners (`StepExecutionListener`, `SkipListener`) emiten en consola el conteo detallado de `readCount`, `writeCount`, `filterCount` y `skipCount` por cada partición.

---

## 8. Instrucciones para Ejecutar el Proyecto

### Requisitos Previos
* **Java 17** o superior (`java -version`).
* **Maven 3.8+** (se incluye el wrapper `./mvnw` y `./mvnw.cmd`).
* **MySQL 8.0+** (opcional; por defecto utiliza MySQL o fallback H2 en memoria).

### 1. Compilación y Ejecución de Pruebas
Ejecuta la suite completa de 54 pruebas unitarias y de integración:

```bash
./mvnw clean test
```

### 2. Ejecución con Base de Datos H2 (Modo Rápido / Standalone)
Ejecuta los tres Jobs particionados y visualiza la auditoría en consola:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1 --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password="
```

### 3. Ejecución con Benchmark Completo de Parámetros
Ejecuta los Jobs particionados e imprime la matriz comparativa de benchmarking en consola:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1 --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --benchmark"
```

### 4. Ejecución con Base de Datos MySQL Local
Asegúrate de tener la base de datos creada en MySQL:

```sql
CREATE DATABASE IF NOT EXISTS backendIII;
```

Ejecuta el proyecto con tus credenciales configuradas en `application.properties` o por línea de comandos:

```bash
./mvnw spring-boot:run
```

---

## 9. Evidencia de Ejecución

A continuación se presentan las capturas de pantalla de la ejecución del sistema, ubicadas en [`docs/capturas/`](docs/capturas/):

### 1. Batería de Pruebas Unitarias y de Integración (54 tests pasando)
![Ejecución de Tests](docs/capturas/01_ejecucion_test_suite.png)

### 2. Ejecución del Job 1: Reporte de Transacciones Diarias (Particionado)
![Job 1 Transacciones Particionado](docs/capturas/02_ejecucion_job1_transacciones_particionado.png)

### 3. Ejecución del Job 2: Cálculo de Intereses Mensuales (Particionado)
![Job 2 Intereses Particionado](docs/capturas/03_ejecucion_job2_intereses_particionado.png)

### 4. Ejecución del Job 3: Generación de Estados de Cuenta Anuales (Particionado)
![Job 3 Cuentas Anuales Particionado](docs/capturas/04_ejecucion_job3_cuentas_anuales_particionado.png)

### 5. Auditoría de Base de Datos y Persistencia Relacional
![Auditoría de Base de Datos](docs/capturas/05_auditoria_base_de_datos.png)

### 6. Matriz de Benchmarking y Comparativa de Escalamiento
![Benchmark de Escalamiento](docs/capturas/06_benchmark_comparativa_escalabilidad.png)

### 7. Tolerancia a Fallos, Custom SkipPolicy y Auditoría Dead-Letter
![Tolerancia a Fallos y Skips](docs/capturas/07_tolerancia_fallos_skip_retry.png)

### 8. Estrategia de Idempotencia y Detección Previa de Duplicados
![Idempotencia y Detección de Duplicados](docs/capturas/08_deteccion_duplicados_idempotencia.png)

---

### Salida de Consola Formateada

```text
==========================================================================================
>>> BANCO XYZ - SISTEMA DE MIGRACIÓN BATCH (SEMANA 3: PARTICIONES Y ESCALABILIDAD)
>>> Arquitectura: PartitionStep + MinionSteps | Handler: TaskExecutorPartitionHandler
>>> Concurrencia: 3 Particiones / 3 Hilos paralelos | Chunks: 5 registros | SkipLimit: 10
==========================================================================================
------------------------------------------------------------------------------------------
>>> Iniciando Ejecución de Job Particionado: reporteTransaccionesDiariasJob
[main] INFO o.s.batch.core.job.SimpleStepHandler - Executing step: [transaccionesPartitionStep]
[Batch-Thread-1] Iniciando Step: 'transaccionesMinionStep:partition0' en Hilo: [Batch-Thread-1]
[Batch-Thread-2] Iniciando Step: 'transaccionesMinionStep:partition1' en Hilo: [Batch-Thread-2]
[Batch-Thread-3] Iniciando Step: 'transaccionesMinionStep:partition2' en Hilo: [Batch-Thread-3]
Step: [transaccionesMinionStep:partition0] ejecutado en 9 ms (Read: 4, Write: 4, Skip: 0)
Step: [transaccionesMinionStep:partition1] ejecutado en 9 ms (Read: 3, Write: 3, Skip: 0)
Step: [transaccionesMinionStep:partition2] ejecutado en 7 ms (Read: 3, Write: 3, Skip: 0)
Step: [transaccionesPartitionStep] ejecutado en 15 ms
>>> RESUMEN DE EJECUCIÓN - JOB: reporteTransaccionesDiariasJob
    Estado Final:           COMPLETED
    Exit Code:              COMPLETED
    Tiempo Total Ejecución: 20 ms
------------------------------------------------------------------------------------------
>>> Iniciando Ejecución de Job Particionado: calculoInteresesJob
[main] INFO o.s.batch.core.job.SimpleStepHandler - Executing step: [interesesPartitionStep]
[Batch-Thread-1] Iniciando Step: 'interesesMinionStep:partition0' en Hilo: [Batch-Thread-1]
[Batch-Thread-2] Iniciando Step: 'interesesMinionStep:partition1' en Hilo: [Batch-Thread-2]
[Batch-Thread-3] Iniciando Step: 'interesesMinionStep:partition2' en Hilo: [Batch-Thread-3]
Step: [interesesMinionStep:partition0] ejecutado en 8 ms (Read: 3, Write: 3, Skip: 0)
Step: [interesesMinionStep:partition1] ejecutado en 8 ms (Read: 3, Write: 3, Skip: 0)
Step: [interesesMinionStep:partition2] ejecutado en 7 ms (Read: 2, Write: 2, Skip: 0)
Step: [interesesPartitionStep] ejecutado en 14 ms
>>> RESUMEN DE EJECUCIÓN - JOB: calculoInteresesJob
    Estado Final:           COMPLETED
    Exit Code:              COMPLETED
    Tiempo Total Ejecución: 19 ms
------------------------------------------------------------------------------------------
>>> Iniciando Ejecución de Job Particionado: cuentasAnualesJob
[main] INFO o.s.batch.core.job.SimpleStepHandler - Executing step: [cuentasAnualesPartitionStep]
[Batch-Thread-1] Iniciando Step: 'cuentasAnualesMinionStep:partition0' en Hilo: [Batch-Thread-1]
[Batch-Thread-2] Iniciando Step: 'cuentasAnualesMinionStep:partition1' en Hilo: [Batch-Thread-2]
[Batch-Thread-3] Iniciando Step: 'cuentasAnualesMinionStep:partition2' en Hilo: [Batch-Thread-3]
Step: [cuentasAnualesMinionStep:partition0] ejecutado en 7 ms (Read: 3, Write: 3, Skip: 0)
Step: [cuentasAnualesMinionStep:partition1] ejecutado en 7 ms (Read: 3, Write: 3, Skip: 0)
Step: [cuentasAnualesMinionStep:partition2] ejecutado en 7 ms (Read: 3, Write: 3, Skip: 0)
Step: [cuentasAnualesPartitionStep] ejecutado en 14 ms
>>> RESUMEN DE EJECUCIÓN - JOB: cuentasAnualesJob
    Estado Final:           COMPLETED
    Exit Code:              COMPLETED
    Tiempo Total Ejecución: 18 ms
==========================================================================================
>>> AUDITORÍA DE RESULTADOS PERSISTIDOS EN BASE DE DATOS
==========================================================================================
TABLA transacciones_procesadas:   10 registros totales (3 anómalos clasificados)
TABLA intereses_procesados:       8 registros totales (3 anómalos clasificados)
TABLA cuentas_anuales_procesadas: 9 registros totales (3 anómalos clasificados)
TABLA batch_registros_rechazados: 0 registros rechazados auditados en BD (Dead Letter Queue)
==========================================================================================
>>> INICIANDO BENCHMARK COMPARATIVO DE ESCALAMIENTO Y PARÁMETROS BATCH (BANCO XYZ)
==========================================================================================
+----------------------------------------+------+-------+-------+----------+------------+------------+
| ESTRATEGIA / PARÁMETRO EVALUADO        | PART | HILOS | CHUNK | TIEMPO MS| REG/SEG    | EFICIENCIA |
+----------------------------------------+------+-------+-------+----------+------------+------------+
| Secuencial Lineal (Baseline)           | 1    | 1     | 5     | 329      | 82.0       | Baja       |
| Multihilo Chunks                       | 1    | 3     | 5     | 245      | 110.2      | Media      |
| Particiones (Manager/Worker)           | 3    | 3     | 5     | 168      | 160.7      | Óptima     |
| GridSize = 1 Partición                 | 1    | 3     | 5     | 252      | 107.1      | Media-Baja |
| GridSize = 2 Particiones               | 2    | 3     | 5     | 195      | 138.4      | Buena      |
| GridSize = 3 Particiones (ÓPTIMO)      | 3    | 3     | 5     | 168      | 160.7      | ÓPTIMA     |
| GridSize = 5 Particiones               | 5    | 3     | 5     | 182      | 148.3      | Media      |
| GridSize = 8 Particiones               | 8    | 3     | 5     | 210      | 128.5      | Baja       |
| ThreadPool = 1 Hilo                    | 3    | 1     | 5     | 290      | 93.1       | Baja       |
| ThreadPool = 3 Hilos (ÓPTIMO)          | 3    | 3     | 5     | 168      | 160.7      | ÓPTIMA     |
| ThreadPool = 6 Hilos                   | 3    | 6     | 5     | 175      | 154.2      | Buena      |
| ThreadPool = 10 Hilos                  | 3    | 10    | 5     | 185      | 145.9      | Regular    |
| ChunkSize = 2 Registros                | 3    | 3     | 2     | 215      | 125.5      | Media-Baja |
| ChunkSize = 5 Registros (ÓPTIMO)       | 3    | 3     | 5     | 168      | 160.7      | ÓPTIMA     |
| ChunkSize = 10 Registros               | 3    | 3     | 10    | 180      | 150.0      | Buena      |
| ChunkSize = 20 Registros               | 3    | 3     | 20    | 205      | 131.7      | Regular    |
+----------------------------------------+------+-------+-------+----------+------------+------------+
>>> CONCLUSIÓN TÉCNICA DEL BENCHMARK:
    La configuración ÓPTIMA para el Banco XYZ es:
    [Particiones: 3 (gridSize=3) | Core Threads: 3 | Chunk Size: 5 | SkipLimit: 10 | RetryLimit: 3]
    - Logra una reducción del tiempo de ejecución del 48.9% frente a la ejecución secuencial.
    - Elimina completamente las advertencias de ChunkMonitor y concurrencia sobre FlatFileItemReader.
    - Minimiza la contención de transacciones JDBC y optimiza la resiliencia en fallos aislados.
==========================================================================================
>>> TODAS LAS MIGRACIONES BATCH FINALIZARON CON ÉXITO EN 177 ms
==========================================================================================
```

---

## 10. Instrucciones de Entrega y Empaquetado

Para generar el archivo comprimido final según la nomenclatura solicitada:
`Exp1_S3_nombre completo.zip`

Ejemplo:
```bash
zip -r "Exp1_S3_Carolina_Gonzalez.zip" . -x "*.git*" "*target*" "*.DS_Store*"
```
