# PostgreSQL local provisional — Ultima Shop

Configuración **solo para desarrollo local** de Jean. No es la infraestructura completa de Bryan: no hay API ni worker en Compose, ni RabbitMQ, ni CI/CD. Los archivos de esta carpeta pueden convivir con el trabajo posterior de Bryan en `infra/`.

El nombre del proyecto Compose es `ultimashop-local-dev` y el volumen `ultimashop-local-dev-postgres-data`, para no colisionar con otros entornos.

## Comprobación previa realizada el 2026-11-18

Antes de crear nada se verificó:

- `docker version`: cliente y engine 29.9.3, Docker Desktop 4.99.0, contexto `desktop-linux`. El motor respondió.
- `docker compose version`: v5.7.0.
- `docker ps -a`, `docker volume ls` y `docker network ls`: sin contenedores, volúmenes ni redes de usuario previos.
- Puertos 5432 y 8080: libres.

No se sobrescribió ni borró nada.

## Archivos

- `compose.yaml`: un solo servicio `postgres` con imagen `postgres:16-alpine`, healthcheck `pg_isready`, volumen persistente y enlace `127.0.0.1:5432:5432` (no expuesto a la red LAN).
- `.env.example`: plantilla sin secretos.
- `.env`: credenciales locales generadas para esta demostración. Está ignorado por Git; **no subir ni compartir**.
- `.env.brevo`: configuración privada de Brevo (`BREVO_API_KEY`, `MAIL_FROM_EMAIL`, `MAIL_FROM_NAME`, `APP_BASE_URL`, `MAIL_ENABLED`). Está ignorado por Git; **no subir ni compartir**.
- `cargar-brevo.ps1`: cargador que exporta esas claves como variables de entorno del proceso **sin imprimir valores** y fuerza `MAIL_ENABLED=false`. No contiene secretos.
- `.gitignore`: excluye `.env` y variantes, conservando `.env.example`.

La API espera `DB_URL`, `DB_USER` y `DB_PASSWORD`. La URL usada es `jdbc:postgresql://localhost:5432/ultimashop_dev`. Para el acceso con sesión también requiere `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` y `APP_ADMIN_NAME`; el usuario se crea solo la primera vez y la contraseña se guarda como hash BCrypt. `COOKIE_SECURE=false` es adecuado para este HTTP local.

## Iniciar desde PowerShell

Primero, desde esta carpeta:

```powershell
Set-Location 'C:\Jean_Project\Proaula_2026\local-dev'
docker compose up -d
docker compose ps
```

Esperar a que `STATUS` diga `(healthy)`. Luego, en otra terminal:

```powershell
$values = @{}
Get-Content 'C:\Jean_Project\Proaula_2026\local-dev\.env' | Where-Object { $_ -and -not $_.StartsWith('#') -and $_ -match '^([^=]+)=(.*)$' } | ForEach-Object { $values[$Matches[1]] = $Matches[2] }

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DB_URL = "jdbc:postgresql://localhost:5432/$($values['DB_NAME'])"
$env:DB_USER = $values['DB_USER']
$env:DB_PASSWORD = $values['DB_PASSWORD']
$env:APP_ADMIN_USERNAME = 'admin'
$env:APP_ADMIN_PASSWORD = 'REEMPLAZAR_CON_VALOR_LOCAL'
$env:APP_ADMIN_NAME = 'Admin Demo'
$env:COOKIE_SECURE = 'false'

Set-Location 'C:\Jean_Project\Proaula_2026\api'
.\mvnw.cmd spring-boot:run
```

`JAVA_HOME` se ajusta solo en la sesión; no se cambió la configuración global del PC.

## Correo Brevo: configuración preparada, envíos NO activados

Los valores privados viven en `.env.brevo` y no se muestran. Para cargarlos en la misma sesión de PowerShell, desde esta carpeta:

```powershell
Set-Location 'C:\Jean_Project\Proaula_2026\local-dev'
. .\cargar-brevo.ps1
```

El punto inicial es necesario: sin él las variables mueren al terminar el script. El cargador imprime solo los nombres de las variables cargadas, nunca sus valores.

**Seguridad:** el cargador fuerza `MAIL_ENABLED=false` al final, incluso si `.env.brevo` contiene otro valor. Por tanto, deja la configuración lista pero **no envía correos reales**. Para habilitar envíos hace falta una instrucción explícita y un destinatario de prueba. El detalle de variables y timeouts está en `../api/README.md`.

## Verificaciones realizadas el 2026-11-18

- `docker compose config --quiet`: configuración válida.
- `docker compose up -d`: contenedor `ultimashop-local-dev-postgres` creado; red `ultimashop-local-dev_default` y volumen `ultimashop-local-dev-postgres-data` creados.
- `docker compose ps`: `Up (healthy)` y `127.0.0.1:5432->5432/tcp`.
- `Get-NetTCPConnection`: 5432 en escucha solo en `127.0.0.1`.
- API con Java 21 temporal: `Started UltimaShopApiApplication`; Tomcat en 8080; sin errores en el arranque.
- Flyway: log `Successfully applied 1 migration ... now at version v1`. Consulta en la base: `1|1|baseline|true`.
- `GET http://localhost:8080/actuator/health`: HTTP 200, cuerpo `{"status":"UP"}` (sin secretos).
- `GET http://localhost:8080/`: HTTP 200; contiene Ultima Shop, Dashboard, Ventas, Inventario, Servicios, Movimientos y Pendiente.

## Detener conservando los datos

Detener solo la API: presionar `Ctrl+C` en la terminal de `spring-boot:run`.

Detener PostgreSQL sin borrar datos:

```powershell
Set-Location 'C:\Jean_Project\Proaula_2026\local-dev'
docker compose stop
```

Volver a iniciar con los mismos datos:

```powershell
docker compose start
docker compose ps
```

`docker compose down` también conserva el volumen, pero **no usar** `docker compose down -v` ni `docker volume rm`, porque eso borraría la base de pruebas. Para comprobar la persistencia: `docker volume inspect ultimashop-local-dev-postgres-data` y, tras reiniciar, consultar otra vez `flyway_schema_history`.

## Estado actual

- La API incluye los cinco módulos y las migraciones V1 a V6. La base local conserva los datos existentes; Flyway aplica las migraciones nuevas sin borrarlos.
- El 2026-11-19 se ejecutó un recorrido ficticio en vivo: venta mixta id 1 ($ 97.50), salida de stock y evento `VentaRegistrada` v1 en outbox, sin publicación. El detalle y los identificadores están en `../api/README.md`.
- Testcontainers usa su propia base efímera; nunca se conecta a este volumen.
- La integración definitiva de Bryan en `infra/` sigue pendiente. Esta carpeta es provisional y no debe reemplazarla.
- El 2026-11-19 se agregó `.env.brevo` (privado, ignorado) y `cargar-brevo.ps1`. Se verificó su carga en sesión aislada: las cinco claves se exportaron, no se imprimieron valores y `MAIL_ENABLED` quedó en `false`. **No se activaron envíos reales.**
- No se crearon commits ni push.
