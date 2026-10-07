package com.bancoxyz.migracion.repository;

import com.bancoxyz.migracion.model.BatchRegistroRechazado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JDBC para registrar y auditar los registros omitidos/rechazados en la tabla batch_registros_rechazados.
 */
@Repository
public class BatchRegistroRechazadoRepository {

    private static final Logger log = LoggerFactory.getLogger(BatchRegistroRechazadoRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public BatchRegistroRechazadoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void guardar(BatchRegistroRechazado registro) {
        String sql = "INSERT INTO batch_registros_rechazados " +
                "(job_name, step_name, identificador_registro, datos_origen, tipo_error, motivo_rechazo, fecha_registro) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            LocalDateTime fecha = registro.getFechaRegistro() != null ? registro.getFechaRegistro() : LocalDateTime.now();
            jdbcTemplate.update(sql,
                    registro.getJobName(),
                    registro.getStepName(),
                    registro.getIdentificadorRegistro(),
                    registro.getDatosOrigen(),
                    registro.getTipoError(),
                    registro.getMotivoRechazo(),
                    Timestamp.valueOf(fecha));
            log.info("Registro rechazado guardado en BD (batch_registros_rechazados): job={}, step={}, id={}, error={}",
                    registro.getJobName(), registro.getStepName(), registro.getIdentificadorRegistro(), registro.getTipoError());
        } catch (Exception e) {
            log.error("Error al persistir registro rechazado en la tabla batch_registros_rechazados: {}", e.getMessage(), e);
        }
    }

    public long contarRegistrosRechazados() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch_registros_rechazados", Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Error al contar registros en batch_registros_rechazados: {}", e.getMessage());
            return 0L;
        }
    }

    public List<BatchRegistroRechazado> obtenerTodos() {
        String sql = "SELECT id, job_name, step_name, identificador_registro, datos_origen, tipo_error, motivo_rechazo, fecha_registro " +
                "FROM batch_registros_rechazados ORDER BY id ASC";
        return jdbcTemplate.query(sql, new BatchRegistroRechazadoRowMapper());
    }

    private static class BatchRegistroRechazadoRowMapper implements RowMapper<BatchRegistroRechazado> {
        @Override
        public BatchRegistroRechazado mapRow(ResultSet rs, int rowNum) throws SQLException {
            BatchRegistroRechazado r = new BatchRegistroRechazado();
            r.setId(rs.getLong("id"));
            r.setJobName(rs.getString("job_name"));
            r.setStepName(rs.getString("step_name"));
            r.setIdentificadorRegistro(rs.getString("identificador_registro"));
            r.setDatosOrigen(rs.getString("datos_origen"));
            r.setTipoError(rs.getString("tipo_error"));
            r.setMotivoRechazo(rs.getString("motivo_rechazo"));
            Timestamp ts = rs.getTimestamp("fecha_registro");
            if (ts != null) {
                r.setFechaRegistro(ts.toLocalDateTime());
            }
            return r;
        }
    }
}
