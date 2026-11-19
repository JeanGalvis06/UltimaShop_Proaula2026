-- La API administra el esquema; el worker aplica efectos transaccionales idempotentes.
ALTER TABLE outbox_eventos ADD COLUMN proximo_intento_en TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE TABLE eventos_procesados (
    consumidor VARCHAR(40) NOT NULL,
    event_id UUID NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(consumidor,event_id)
);
CREATE TABLE comprobantes (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL UNIQUE REFERENCES ventas(id),
    event_id UUID NOT NULL UNIQUE,
    numero VARCHAR(40) NOT NULL UNIQUE,
    total NUMERIC(14,2) NOT NULL CHECK(total>=0),
    moneda VARCHAR(3) NOT NULL,
    detalle JSONB NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE ventas_proyectadas (
    venta_id BIGINT PRIMARY KEY REFERENCES ventas(id),
    event_id UUID NOT NULL UNIQUE,
    dia DATE NOT NULL
);
CREATE TABLE resumen_ventas_diario (
    dia DATE PRIMARY KEY,
    ventas BIGINT NOT NULL CHECK(ventas>=0),
    total NUMERIC(18,2) NOT NULL CHECK(total>=0),
    total_productos NUMERIC(18,2) NOT NULL CHECK(total_productos>=0),
    total_servicios NUMERIC(18,2) NOT NULL CHECK(total_servicios>=0),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK(total=total_productos+total_servicios)
);
