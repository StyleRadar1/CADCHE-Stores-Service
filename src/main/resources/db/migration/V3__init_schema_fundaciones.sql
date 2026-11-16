-- V3__init_schema_fundaciones.sql
-- Creación del esquema para fundaciones benéficas aliadas (SR-RF-63, SR-RF-64, SR-RF-65)

CREATE TABLE IF NOT EXISTS fundaciones (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(500),
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(100) NOT NULL,
    latitud DOUBLE PRECISION NOT NULL,
    longitud DOUBLE PRECISION NOT NULL,
    representante_id BIGINT NOT NULL,
    url_certificado_personeria_juridica VARCHAR(300),
    causas_apoyadas VARCHAR(250),
    horario_atencion VARCHAR(150),
    politicas_donacion VARCHAR(500),
    activa BOOLEAN DEFAULT TRUE NOT NULL,
    fecha_registro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS fundacion_categorias_aceptadas (
    fundacion_id BIGINT NOT NULL REFERENCES fundaciones(id) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    CONSTRAINT uk_fundacion_categoria UNIQUE (fundacion_id, categoria)
);

CREATE INDEX IF NOT EXISTS idx_fundaciones_nombre ON fundaciones(nombre);
CREATE INDEX IF NOT EXISTS idx_fundaciones_representante ON fundaciones(representante_id);
CREATE INDEX IF NOT EXISTS idx_fundaciones_activa ON fundaciones(activa);
