-- Actividad 2.3: roles, correo y soporte de invalidación de sesiones.
-- Conserva usuarios existentes: no restablece contraseñas ni exige nueva verificación.
ALTER TABLE app_users
    ADD COLUMN email VARCHAR(254),
    ADD COLUMN email_normalizado VARCHAR(254),
    ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sesiones_invalidas_desde TIMESTAMPTZ;

-- Las cuentas creadas por el inicializador anterior usaban ROLE_USER como administrador local.
-- Se conservan activas y verificadas para no bloquear el acceso actual.
UPDATE app_users
SET rol = 'ROLE_ADMIN',
    correo_verificado = TRUE
WHERE rol = 'ROLE_USER';

CREATE UNIQUE INDEX ux_app_users_email_normalizado
    ON app_users (email_normalizado)
    WHERE email_normalizado IS NOT NULL;

CREATE INDEX idx_app_users_rol_activo ON app_users (rol, activo);
