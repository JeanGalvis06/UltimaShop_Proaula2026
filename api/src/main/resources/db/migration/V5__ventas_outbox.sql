-- Actividad 2.2: ventas, detalle histórico, idempotencia y outbox.
-- El publicador y los consumidores son responsabilidad de Lewis; aquí solo se guarda el evento.
CREATE TABLE ventas (
    id                  BIGSERIAL PRIMARY KEY,
    clave_idempotencia  VARCHAR(80) NOT NULL,
    solicitud_hash      VARCHAR(64) NOT NULL,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    total               NUMERIC(14,2) NOT NULL CHECK (total >= 0),
    total_productos     NUMERIC(14,2) NOT NULL CHECK (total_productos >= 0),
    total_servicios     NUMERIC(14,2) NOT NULL CHECK (total_servicios >= 0),
    moneda              VARCHAR(3) NOT NULL DEFAULT 'COP',
    usuario_id          BIGINT REFERENCES app_users(id),
    usuario_nombre      VARCHAR(100),
    CONSTRAINT uk_ventas_clave_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT chk_venta_estado CHECK (estado = 'CONFIRMADA'),
    CONSTRAINT chk_venta_totales CHECK (total = total_productos + total_servicios)
);

CREATE TABLE venta_detalles (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES ventas(id),
    tipo            VARCHAR(10) NOT NULL CHECK (tipo IN ('PRODUCTO', 'SERVICIO')),
    referencia_id   BIGINT NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    codigo          VARCHAR(40) NOT NULL,
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal        NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT chk_detalle_subtotal CHECK (subtotal = precio_unitario * cantidad),
    CONSTRAINT uk_detalle_por_referencia UNIQUE (venta_id, tipo, referencia_id)
);

CREATE TABLE outbox_eventos (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL,
    event_type      VARCHAR(80) NOT NULL,
    schema_version  INTEGER NOT NULL,
    exchange_name   VARCHAR(80) NOT NULL,
    routing_key     VARCHAR(80) NOT NULL,
    aggregate_type  VARCHAR(40) NOT NULL,
    aggregate_id    BIGINT NOT NULL,
    payload         JSONB NOT NULL,
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos        INTEGER NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    publicado_en    TIMESTAMPTZ,
    ultimo_error    TEXT,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
    CONSTRAINT chk_outbox_estado CHECK (estado IN ('PENDIENTE', 'PUBLICADO', 'FALLIDO'))
);

CREATE INDEX idx_ventas_fecha ON ventas(fecha DESC, id DESC);
CREATE INDEX idx_venta_detalles_venta_id ON venta_detalles(venta_id);
CREATE INDEX idx_outbox_estado_id ON outbox_eventos(estado, id);
