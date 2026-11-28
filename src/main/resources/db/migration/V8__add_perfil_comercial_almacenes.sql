-- V8__add_perfil_comercial_almacenes.sql
-- Agrega campos del perfil comercial editable del almacén aliado

ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS representante_legal VARCHAR(120);
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS cuenta_pagos VARCHAR(120);
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS direccion_comercial VARCHAR(200);
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS barrio VARCHAR(80);
ALTER TABLE almacenes ADD COLUMN IF NOT EXISTS horario_atencion VARCHAR(150);
