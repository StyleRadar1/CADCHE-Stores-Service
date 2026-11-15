-- V5__add_nit_barrio_fundaciones.sql
-- Agrega nit (único) y barrio a fundaciones benéficas aliadas (SR-RF-63, SR-RF-64)

ALTER TABLE fundaciones ADD COLUMN IF NOT EXISTS nit VARCHAR(20);
ALTER TABLE fundaciones ADD COLUMN IF NOT EXISTS barrio VARCHAR(80);

UPDATE fundaciones SET nit = CONCAT('90000000', id, '-1') WHERE nit IS NULL;
UPDATE fundaciones SET barrio = 'Chapinero' WHERE barrio IS NULL;

ALTER TABLE fundaciones ALTER COLUMN nit SET NOT NULL;
ALTER TABLE fundaciones ALTER COLUMN barrio SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_fundaciones_nit ON fundaciones(nit);

