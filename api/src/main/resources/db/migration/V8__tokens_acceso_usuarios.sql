-- Actividad 2.3: tokens de invitación y recuperación (hash, un solo uso y vencimiento).
CREATE TABLE tokens_acceso_usuario (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES app_users(id),
    proposito       VARCHAR(20) NOT NULL,
    token_hash      VARCHAR(64) NOT NULL,
    expira_en       TIMESTAMPTZ NOT NULL,
    consumido_en    TIMESTAMPTZ,
    invalidado_en   TIMESTAMPTZ,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    solicitado_ip   VARCHAR(45),
    CONSTRAINT uk_tokens_acceso_hash UNIQUE (token_hash),
    CONSTRAINT chk_tokens_acceso_proposito CHECK (proposito IN ('INVITACION', 'RECUPERACION'))
);

CREATE INDEX idx_tokens_acceso_usuario_proposito
    ON tokens_acceso_usuario (usuario_id, proposito, creado_en DESC);
CREATE INDEX idx_tokens_acceso_expira_en
    ON tokens_acceso_usuario (expira_en);
