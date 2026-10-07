CREATE TABLE IF NOT EXISTS transacciones_procesadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaccion_id VARCHAR(50),
    fecha_transaccion DATE,          
    monto DECIMAL(15, 2),
    tipo_transaccion VARCHAR(20),
    es_anomala BOOLEAN DEFAULT FALSE,
    motivo_observacion TEXT
);

-- 2. Tabla para el Job 2 (intereses.csv)
CREATE TABLE IF NOT EXISTS intereses_procesados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id VARCHAR(50),
    nombre VARCHAR(100),
    saldo DECIMAL(15, 2),
    tipo VARCHAR(50),
    interes_calculado DECIMAL(15, 2),
    saldo_final DECIMAL(15, 2),
    es_anomala BOOLEAN DEFAULT FALSE,
    motivo_observacion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS cuentas_anuales_procesadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id VARCHAR(50),
    fecha DATE,
    transaccion VARCHAR(50),
    monto DECIMAL(15, 2),
    descripcion VARCHAR(255),
    es_anomala BOOLEAN DEFAULT FALSE,
    motivo_observacion VARCHAR(255)
);

-- 4. Tabla de Auditoría / Rechazos (Dead Letter Table para Spring Batch Skip)
CREATE TABLE IF NOT EXISTS batch_registros_rechazados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100),
    step_name VARCHAR(100),
    identificador_registro VARCHAR(100),
    datos_origen TEXT,
    tipo_error VARCHAR(100),
    motivo_rechazo TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
