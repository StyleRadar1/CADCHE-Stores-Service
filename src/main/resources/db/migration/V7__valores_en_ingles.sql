-- Análisis de requerimientos v2 (17/11/2026): los valores que viajan en la API quedan en inglés.
-- Se traducen los datos existentes para que coincidan con los enums nuevos.

UPDATE almacenes SET estado_verificacion = 'PENDING'  WHERE estado_verificacion = 'PENDIENTE';
UPDATE almacenes SET estado_verificacion = 'APPROVED' WHERE estado_verificacion = 'APROBADO';
UPDATE almacenes SET estado_verificacion = 'REJECTED' WHERE estado_verificacion = 'RECHAZADO';
ALTER TABLE almacenes ALTER COLUMN estado_verificacion SET DEFAULT 'PENDING';

UPDATE almacen_categorias SET categoria = 'SPORTY'      WHERE categoria = 'DEPORTIVA';
UPDATE almacen_categorias SET categoria = 'ACCESSORIES' WHERE categoria = 'ACCESORIOS';

UPDATE fundacion_categorias_aceptadas SET categoria = 'TOP'       WHERE categoria = 'SUPERIOR';
UPDATE fundacion_categorias_aceptadas SET categoria = 'BOTTOM'    WHERE categoria = 'INFERIOR';
UPDATE fundacion_categorias_aceptadas SET categoria = 'FOOTWEAR'  WHERE categoria = 'CALZADO';
UPDATE fundacion_categorias_aceptadas SET categoria = 'DRESS'     WHERE categoria = 'VESTIDO';
UPDATE fundacion_categorias_aceptadas SET categoria = 'ACCESSORY' WHERE categoria = 'ACCESORIO';
