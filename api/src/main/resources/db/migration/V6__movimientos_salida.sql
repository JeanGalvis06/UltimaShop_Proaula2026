-- Actividad 2.2: SALIDA por venta y auditabilidad de movimientos nuevos.
-- Los movimientos anteriores conservan usuario y venta en NULL; no se inventan autores.
ALTER TABLE movimientos_inventario
    DROP CONSTRAINT IF EXISTS movimientos_inventario_tipo_check,
    ADD COLUMN venta_id BIGINT REFERENCES ventas(id),
    ADD COLUMN usuario_id BIGINT REFERENCES app_users(id),
    ADD COLUMN usuario_nombre VARCHAR(100);

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_tipo_check
        CHECK (tipo IN ('ENTRADA', 'AJUSTE', 'SALIDA'));

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT chk_salida_negativa CHECK (tipo <> 'SALIDA' OR cambio < 0);

CREATE INDEX idx_movimientos_fecha ON movimientos_inventario(fecha DESC, id DESC);
CREATE INDEX idx_movimientos_tipo_fecha ON movimientos_inventario(tipo, fecha DESC);
CREATE INDEX idx_movimientos_venta_id ON movimientos_inventario(venta_id);
