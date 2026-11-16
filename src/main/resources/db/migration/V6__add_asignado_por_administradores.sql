-- CADCHE-130: guarda qué administrador de StyleRadar hizo cada asociación (antes se respondía un valor fijo).
-- Los vínculos creados al registrar el almacén y los anteriores a esta versión quedan en NULL.
ALTER TABLE administradores_almacenes ADD COLUMN IF NOT EXISTS asignado_por_id BIGINT;
