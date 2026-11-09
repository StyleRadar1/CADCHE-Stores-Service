-- V4__add_fotos_tienda.sql
-- Módulo multimedia para tiendas físicas: soporte para hasta 6 fotografías de la tienda y portada (SR-RF-74)

ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS foto_portada_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS almacen_fotos (
    id BIGSERIAL PRIMARY KEY,
    almacen_nit VARCHAR(30) NOT NULL REFERENCES almacenes(nit) ON DELETE CASCADE,
    url_foto VARCHAR(500) NOT NULL,
    orden INT NOT NULL,
    es_portada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_subida TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_almacen_fotos_nit ON almacen_fotos(almacen_nit);
CREATE INDEX IF NOT EXISTS idx_almacen_fotos_orden ON almacen_fotos(almacen_nit, orden);

