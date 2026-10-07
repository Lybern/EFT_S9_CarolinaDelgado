package com.bancoxyz.migracion.listener;

import com.bancoxyz.migracion.dto.CuentaAnualDTO;
import com.bancoxyz.migracion.dto.CuentaInteresDTO;
import com.bancoxyz.migracion.dto.TransaccionDTO;
import com.bancoxyz.migracion.model.BatchRegistroRechazado;
import com.bancoxyz.migracion.model.CuentaAnualProcesada;
import com.bancoxyz.migracion.model.CuentaInteresProcesada;
import com.bancoxyz.migracion.model.TransaccionProcesada;
import com.bancoxyz.migracion.repository.BatchRegistroRechazadoRepository;
import com.bancoxyz.migracion.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Clase base para la auditoría de omisiones (skips) en la tabla batch_registros_rechazados,
 * archivos CSV y consola.
 */
public abstract class AbstractBatchSkipListener<I, O> implements SkipListener<I, O>, StepExecutionListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String ERROR_FILE_PATH = "data/errores.csv";

    protected BatchRegistroRechazadoRepository rejectedRepository;
    protected String defaultJobName;
    protected String defaultStepName;
    protected volatile String currentJobName;
    protected volatile String currentStepName;

    static {
        try {
            Files.createDirectories(Paths.get("data"));
            if (!Files.exists(Paths.get(ERROR_FILE_PATH))) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(ERROR_FILE_PATH, false))) {
                    writer.println("timestamp,fase,registro_o_linea,tipo_excepcion,mensaje_error");
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(AbstractBatchSkipListener.class)
                    .error("No se pudo inicializar el archivo de errores {}: {}", ERROR_FILE_PATH, e.getMessage());
        }
    }

    public AbstractBatchSkipListener() {
    }

    public AbstractBatchSkipListener(BatchRegistroRechazadoRepository rejectedRepository) {
        this.rejectedRepository = rejectedRepository;
    }

    public AbstractBatchSkipListener(BatchRegistroRechazadoRepository rejectedRepository, String defaultJobName, String defaultStepName) {
        this.rejectedRepository = rejectedRepository;
        this.defaultJobName = defaultJobName;
        this.defaultStepName = defaultStepName;
        this.currentJobName = defaultJobName;
        this.currentStepName = defaultStepName;
    }

    @Autowired(required = false)
    public void setRejectedRepository(BatchRegistroRechazadoRepository rejectedRepository) {
        this.rejectedRepository = rejectedRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        if (stepExecution != null) {
            if (stepExecution.getJobExecution() != null && stepExecution.getJobExecution().getJobInstance() != null) {
                this.currentJobName = stepExecution.getJobExecution().getJobInstance().getJobName();
            } else if (this.defaultJobName != null) {
                this.currentJobName = this.defaultJobName;
            }
            this.currentStepName = stepExecution.getStepName();
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.info("Entro a onSkipInRead: {}", t.getClass().getName());
        String detalle = t.getMessage();
        String identificador = "N/A";
        if (t instanceof FlatFileParseException) {
            FlatFileParseException ffpe = (FlatFileParseException) t;
            detalle = ffpe.getInput();
            identificador = "LINEA_" + ffpe.getLineNumber();
            log.warn("Línea omitida debido a un error en la lectura (Línea {}): {}", ffpe.getLineNumber(), ffpe.getInput());
        } else {
            log.warn("Lectura omitida por error: {}", t.getMessage());
        }
        registrarError("LECTURA", identificador, detalle, t);
    }

    @Override
    public void onSkipInProcess(I item, Throwable t) {
        log.info("Tipo de excepción en SkipListener: {}", t.getClass().getName());
        log.warn("SkipListener activado - Error al procesar registro: {}", item);
        log.warn("Causa del fallo en procesamiento: {}", t.getMessage());
        String identificador = extraerIdentificadorEntrada(item);
        String datosOrigen = item != null ? item.toString() : "NULL";
        registrarError("PROCESAMIENTO", identificador, datosOrigen, t);
    }

    @Override
    public void onSkipInWrite(O item, Throwable t) {
        log.error("SkipListener activado - Error al escribir registro en BD: {}", item);
        log.error("Causa del fallo en escritura: {}", t.getMessage());
        String identificador = extraerIdentificadorSalida(item);
        String datosOrigen = item != null ? item.toString() : "NULL";
        registrarError("ESCRITURA", identificador, datosOrigen, t);
    }

    protected void registrarError(String fase, String identificador, String datosOrigen, Throwable t) {
        String job = this.currentJobName != null ? this.currentJobName : (this.defaultJobName != null ? this.defaultJobName : "UNKNOWN_JOB");
        String step = this.currentStepName != null ? this.currentStepName : (this.defaultStepName != null ? this.defaultStepName : "UNKNOWN_STEP");
        String tipoError = t != null ? t.getClass().getSimpleName() : "ErrorDesconocido";
        String motivoRechazo = t != null && t.getMessage() != null ? t.getMessage() : "Sin detalle";

        // 1. Persistencia en la tabla batch_registros_rechazados
        if (rejectedRepository != null) {
            BatchRegistroRechazado rechazo = BatchRegistroRechazado.builder()
                    .jobName(job)
                    .stepName(step)
                    .identificadorRegistro(identificador != null ? identificador : "N/A")
                    .datosOrigen(datosOrigen)
                    .tipoError(tipoError)
                    .motivoRechazo(motivoRechazo)
                    .fechaRegistro(LocalDateTime.now())
                    .build();
            rejectedRepository.guardar(rechazo);
        } else {
            log.warn("BatchRegistroRechazadoRepository no configurado. Se omite inserción en batch_registros_rechazados.");
        }

        // 2. Persistencia en archivo CSV (errores.csv)
        registrarErrorEnArchivo(fase, datosOrigen, t);
    }

    protected synchronized void registrarErrorEnArchivo(String fase, String contenido, Throwable t) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ERROR_FILE_PATH, true))) {
            String timestamp = DateUtil.formatearTimestamp(LocalDateTime.now());
            String contenidoLimpio = (contenido != null ? contenido : "NULL")
                    .replace(",", ";")
                    .replace("\n", " ")
                    .replace("\r", "");
            String mensajeLimpio = (t != null && t.getMessage() != null ? t.getMessage() : "Sin detalle")
                    .replace(",", ";")
                    .replace("\n", " ");
            String tipoError = (t != null) ? t.getClass().getSimpleName() : "Error";
            writer.printf("%s,%s,%s,%s,%s%n",
                    timestamp,
                    fase,
                    contenidoLimpio,
                    tipoError,
                    mensajeLimpio);
            writer.flush();
        } catch (IOException e) {
            log.error("Error al escribir en archivo de errores: {}", e.getMessage());
        }
    }

    protected String extraerIdentificadorEntrada(I item) {
        if (item == null) {
            return "N/A";
        }
        if (item instanceof TransaccionDTO) {
            String id = ((TransaccionDTO) item).getId();
            return (id != null && !id.isBlank()) ? id.trim() : "SIN_ID";
        }
        if (item instanceof CuentaInteresDTO) {
            String id = ((CuentaInteresDTO) item).getCuentaId();
            return (id != null && !id.isBlank()) ? id.trim() : "SIN_ID";
        }
        if (item instanceof CuentaAnualDTO) {
            String id = ((CuentaAnualDTO) item).getCuentaId();
            return (id != null && !id.isBlank()) ? id.trim() : "SIN_ID";
        }
        return item.toString();
    }

    protected String extraerIdentificadorSalida(O item) {
        if (item == null) {
            return "N/A";
        }
        if (item instanceof TransaccionProcesada) {
            String id = ((TransaccionProcesada) item).getTransaccionId();
            return (id != null && !id.isBlank()) ? id.trim() : "SIN_ID";
        }
        if (item instanceof CuentaInteresProcesada) {
            String id = ((CuentaInteresProcesada) item).getCuentaId();
            return (id != null && !id.isBlank()) ? id.trim() : "SIN_ID";
        }
        if (item instanceof CuentaAnualProcesada) {
            Long id = ((CuentaAnualProcesada) item).getCuentaId();
            return id != null ? String.valueOf(id) : "SIN_ID";
        }
        return item.toString();
    }
}
