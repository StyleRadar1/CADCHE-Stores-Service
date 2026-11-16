-- V2__add_verificacion_almacen.sql
-- Soporte para verificación de personería jurídica con NIT y RUT en PDF (SR-RF-62)

ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS estado_verificacion VARCHAR(30) DEFAULT 'PENDIENTE';
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS fecha_decision_verificacion TIMESTAMP WITH TIME ZONE;
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS administrador_decision_id BIGINT;
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS motivo_verificacion VARCHAR(1000);
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS url_rut_pdf VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_almacenes_estado_verif ON almacenes(estado_verificacion);
