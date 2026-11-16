-- V1__init_schema_almacenes.sql
-- Creación inicial del esquema de almacenes y administradores con soporte para umbral de últimas unidades (SR-RF-59)

CREATE TABLE IF NOT EXISTS almacenes (
    nit VARCHAR(30) PRIMARY KEY,
    nombre_comercial VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    telefono VARCHAR(30),
    correo_contacto VARCHAR(100),
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    reputacion DOUBLE PRECISION DEFAULT 0.0,
    umbral_ultimas_unidades INT DEFAULT 3,
    dias_nueva_llegada INT DEFAULT 7,
    fecha_actualizacion_catalogo TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS almacen_categorias (
    almacen_nit VARCHAR(30) NOT NULL REFERENCES almacenes(nit) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    CONSTRAINT uk_almacen_categoria UNIQUE (almacen_nit, categoria)
);

CREATE TABLE IF NOT EXISTS administradores_almacenes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    almacen_nit VARCHAR(30) NOT NULL REFERENCES almacenes(nit) ON DELETE CASCADE,
    fecha_asignacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_almacen UNIQUE (usuario_id, almacen_nit)
);

CREATE INDEX IF NOT EXISTS idx_almacenes_nombre ON almacenes(nombre_comercial);
CREATE INDEX IF NOT EXISTS idx_admin_almacenes_usuario ON administradores_almacenes(usuario_id);

