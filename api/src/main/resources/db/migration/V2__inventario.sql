-- Actividad 2: Inventario basico. V1 queda intacta.
CREATE TABLE productos (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(40) NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(500),
    costo           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo >= 0),
    precio          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio >= 0),
    stock_minimo    INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    stock_actual    INTEGER NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_productos_codigo UNIQUE (codigo)
);

CREATE TABLE movimientos_inventario (
    id                  BIGSERIAL PRIMARY KEY,
    producto_id         BIGINT NOT NULL REFERENCES productos(id),
    tipo                VARCHAR(20) NOT NULL CHECK (tipo IN ('ENTRADA', 'AJUSTE')),
    cantidad_anterior   INTEGER NOT NULL CHECK (cantidad_anterior >= 0),
    cambio              INTEGER NOT NULL,
    cantidad_resultante INTEGER NOT NULL CHECK (cantidad_resultante >= 0),
    fecha               TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo              VARCHAR(300) NOT NULL,
    CONSTRAINT chk_movimiento_consistente
        CHECK (cantidad_anterior + cambio = cantidad_resultante),
    CONSTRAINT chk_entrada_positiva
        CHECK (tipo <> 'ENTRADA' OR cambio > 0)
);

CREATE INDEX idx_movimientos_producto_id ON movimientos_inventario(producto_id, id DESC);
