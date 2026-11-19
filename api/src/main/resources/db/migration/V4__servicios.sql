-- Actividad 2.2: catálogo de servicios. No consumen inventario.
CREATE TABLE servicios (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    precio      NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_servicios_activo_nombre ON servicios(activo, nombre);
