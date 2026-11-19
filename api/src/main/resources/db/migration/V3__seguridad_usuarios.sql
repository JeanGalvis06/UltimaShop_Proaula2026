-- Actividad 2.2: usuarios locales. No se insertan credenciales fijas.
-- El usuario inicial se crea desde variables de entorno en el arranque.
CREATE TABLE app_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(80) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    rol             VARCHAR(40) NOT NULL DEFAULT 'ROLE_USER',
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_users_username UNIQUE (username)
);
