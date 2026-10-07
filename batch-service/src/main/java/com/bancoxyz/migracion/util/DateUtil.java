package com.bancoxyz.migracion.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;

/**
 * Clase utilitaria centralizada para la validación, parseo y formateo de fechas
 * en los procesos batch del Banco XYZ.
 * Soporta múltiples formatos estándar bancarios con validación estricta (ResolverStyle.STRICT):
 * ISO (uuuu-MM-dd), con barras (uuuu/MM/dd), y formatos latinoamericanos (dd-MM-uuuu, dd/MM/uuuu).
 */
public final class DateUtil {

    public static final String PATTERN_YYYY_MM_DD = "uuuu-MM-dd";
    public static final String PATTERN_YYYY_MM_DD_SLASH = "uuuu/MM/dd";
    public static final String PATTERN_DD_MM_YYYY = "dd-MM-uuuu";
    public static final String PATTERN_DD_MM_YYYY_SLASH = "dd/MM/uuuu";
    public static final String PATTERN_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_TIMESTAMP);

    public static final List<DateTimeFormatter> FORMATOS_FECHA_PERMITIDOS = List.of(
            DateTimeFormatter.ofPattern(PATTERN_YYYY_MM_DD).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern(PATTERN_YYYY_MM_DD_SLASH).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern(PATTERN_DD_MM_YYYY).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern(PATTERN_DD_MM_YYYY_SLASH).withResolverStyle(ResolverStyle.STRICT)
    );

    private DateUtil() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Intenta parsear una cadena de texto a LocalDate utilizando los formatos bancarios permitidos con resolución estricta.
     * Retorna null si la cadena es nula, vacía o si ningún formato coincide o si la fecha es calendario inválida.
     *
     * @param fechaStr Cadena que representa la fecha.
     * @return LocalDate resultante o null si no es válida.
     */
    public static LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return null;
        }
        String fechaLimpia = fechaStr.trim();
        for (DateTimeFormatter formatter : FORMATOS_FECHA_PERMITIDOS) {
            try {
                return LocalDate.parse(fechaLimpia, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato
            }
        }
        return null;
    }

    /**
     * Versión Optional de parsearFecha.
     *
     * @param fechaStr Cadena que representa la fecha.
     * @return Optional con el LocalDate resultante.
     */
    public static Optional<LocalDate> parsearFechaOptional(String fechaStr) {
        return Optional.ofNullable(parsearFecha(fechaStr));
    }

    /**
     * Valida si una cadena de texto corresponde a una fecha válida según los formatos configurados.
     *
     * @param fechaStr Cadena a validar.
     * @return true si la fecha es válida y parseable, false en caso contrario.
     */
    public static boolean esFechaValida(String fechaStr) {
        return parsearFecha(fechaStr) != null;
    }

    /**
     * Formatea un LocalDate en el formato deseado.
     *
     * @param fecha LocalDate a formatear.
     * @param patron Patrón de formato (ej. "yyyy-MM-dd").
     * @return Cadena formateada o null si fecha es null.
     */
    public static String formatearFecha(LocalDate fecha, String patron) {
        if (fecha == null || patron == null || patron.isBlank()) {
            return null;
        }
        return fecha.format(DateTimeFormatter.ofPattern(patron));
    }

    /**
     * Formatea un LocalDateTime al formato estándar de timestamp ("yyyy-MM-dd HH:mm:ss").
     *
     * @param dateTime LocalDateTime a formatear.
     * @return Cadena formateada o null si dateTime es null.
     */
    public static String formatearTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(TIMESTAMP_FORMATTER);
    }
}
