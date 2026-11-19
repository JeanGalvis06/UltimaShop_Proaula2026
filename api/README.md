# Ultima Shop API

API REST e interfaz local de Ultima Shop. Incluye Spring MVC, Thymeleaf, Spring Security con sesión, JPA/Flyway para PostgreSQL y Actuator. La aplicación es modular, no una colección de microservicios, y no requiere Node.js ni frontend separado.

Módulos disponibles: **Dashboard, Ventas, Inventario, Servicios, Movimientos y Usuarios**. El inventario y su interfaz morada se conservaron; sobre ellos se agregaron catálogo de servicios, registro comercial transaccional, historial de movimientos, resumen operativo y gestión de acceso con invitaciones, verificación de correo, recuperación de contraseña y perfil. No se implementaron RabbitMQ, publicación del outbox, comprobantes del worker ni infraestructura de despliegue.

## Versiones y decisiones

| Elemento | Versión / valor |
|---|---|
| Java | Temurin 21.0.12.1 (JDK 21) |
| Maven | 3.9.15 (Maven Wrapper) |
| Spring Boot | 3.5.16 (estable, no snapshot) |
| Spring Framework | 6.2.19 (gestionado por Spring Boot) |
| PostgreSQL | 16-alpine (Docker local provisional en `local-dev/` y Testcontainers en pruebas) |
| Testcontainers | 1.21.4 (gestionado por Spring Boot 3.5.16) |
| Grupo / artefacto | `com.ultimashop` / `ultima-shop-api` |

Spring Boot 3.5.16 se eligió de la documentación oficial: versión estable de la línea 3.5, compatible con Java 17 a 25. Fuentes: <https://docs.spring.io/spring-boot/3.5/system-requirements.html>, <https://docs.spring.io/spring-boot/installing.html> y <https://spring.io/projects/spring-boot>.

## Requisitos

- JDK 21 instalado (en este PC: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`).
- PostgreSQL accesible mediante variables de entorno; no hay credenciales predeterminadas.
- Docker para las pruebas de integración (Testcontainers levanta su propia PostgreSQL efímera).
- No se requiere Node.js ni frontend separado.

> En este PC, `JAVA_HOME` apunta a Java 8. Para compilar y ejecutar se usa una configuración **temporal por sesión**, sin cambiar `JAVA_HOME` permanentemente.

## Configuración

Variables obligatorias:

- `DB_URL`: JDBC de la base de desarrollo, por ejemplo `jdbc:postgresql://localhost:5432/ultimashop_dev`.
- `DB_USER`: usuario con permisos sobre esa base.
- `DB_PASSWORD`: contraseña local del usuario (no se guarda en Git).

Variables del usuario inicial:

- `APP_ADMIN_USERNAME`: nombre de acceso.
- `APP_ADMIN_PASSWORD`: contraseña en texto plano que se almacena como hash BCrypt.
- `APP_ADMIN_NAME`: nombre visible en auditoría.

El usuario se crea solo si el nombre no existe. Una vez guardado, un reinicio con otras variables **no** restablece su contraseña ni su nombre. Opcionales generales: `SERVER_PORT` (por defecto `8080`) y `COOKIE_SECURE` (`false` para HTTP local; usar `true` en HTTPS).

Correo transaccional (Brevo):

- `MAIL_ENABLED`: `false` por defecto. Sin esta variable en `true` **no se envían correos reales**.
- `BREVO_API_URL`: por defecto `https://api.brevo.com/v3/smtp/email` (API HTTP oficial con header `api-key`).
- `BREVO_API_KEY` y `MAIL_FROM_EMAIL`: sin valores no se intenta el envío. Nunca se guardan en Git.
- `MAIL_FROM_NAME`: nombre del remitente; por defecto `Ultima Shop`.
- `APP_BASE_URL`: base pública usada para construir enlaces de invitación y recuperación. Nunca se toma del header `Host` recibido.
- `MAIL_CONNECT_TIMEOUT_MS` y `MAIL_READ_TIMEOUT_MS`: `3000` y `5000` por defecto.

Los valores privados están en `../local-dev/.env.brevo` (ignorado por Git). Para exportarlos en la sesión sin mostrarlos, desde `local-dev/`: `. .\cargar-brevo.ps1`. Ese cargador fuerza `MAIL_ENABLED=false`, así que deja la configuración lista pero **no habilita envíos reales**. Hasta que Jean indique remitente y destinatario de prueba, los correos se siguen probando solo contra el servidor simulado de las pruebas.

Tokens y límites:

- `INVITACION_TOKEN_HORAS` (24) y `RECUPERACION_TOKEN_MINUTOS` (30).
- `LOGIN_MAX_CUENTA` (5), `LOGIN_MAX_IP` (15), `LOGIN_VENTANA_MINUTOS` (15).
- `RECUPERACION_MAX_CUENTA` (3), `RECUPERACION_MAX_IP` (10), `RECUPERACION_VENTANA_MINUTOS` (30).
- `INVITACION_MAX_CUENTA` (5), `INVITACION_MAX_IP` (20), `INVITACION_VENTANA_MINUTOS` (60).
- `TRUST_PROXY_HEADERS` (`false`): solo cambiar a `true` detrás de un proxy inverso de confianza; si no, se usa `getRemoteAddr()`.

Los límites viven en memoria: se reinician al reiniciar la aplicación y no se comparten entre réplicas. Los tokens se guardan como hash SHA-256; el valor plano solo viaja en el correo y en el formulario `POST` de confirmación.

En `local-dev/.env` están los valores locales de este PC (ignorado por Git); `api/.env.example` muestra la forma de exportarlos sin credenciales reales.

En PowerShell, dentro de esta carpeta:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DB_URL = 'jdbc:postgresql://localhost:5432/ultimashop_dev'
$env:DB_USER = 'REEMPLAZAR_CON_USUARIO_LOCAL'
$env:DB_PASSWORD = 'REEMPLAZAR_CON_VALOR_LOCAL'
$env:APP_ADMIN_USERNAME = 'admin'
$env:APP_ADMIN_PASSWORD = 'REEMPLAZAR_CON_VALOR_LOCAL'
$env:APP_ADMIN_NAME = 'Admin Demo'
$env:COOKIE_SECURE = 'false'

# Correo: dejar MAIL_ENABLED=false hasta configurar un remitente y destinatario de prueba.
$env:MAIL_ENABLED = 'false'
$env:BREVO_API_KEY = ''
$env:MAIL_FROM_EMAIL = ''
$env:APP_BASE_URL = 'http://localhost:8080'

.\mvnw.cmd -B clean verify
.\mvnw.cmd spring-boot:run
```

También se puede ejecutar el jar empaquetado con `java -jar target\ultima-shop-api-0.0.1-SNAPSHOT.jar`, manteniendo las mismas variables de entorno.

## Seguridad y usuarios

- Login Thymeleaf en `/login`; se acepta usuario o correo normalizado. Las contraseñas se guardan con BCrypt.
- Roles: `ADMIN` gestiona usuarios, productos, servicios, entradas y ajustes; `OPERADOR` registra ventas y puede consultar inventario, servicios, movimientos y dashboard. La autorización se valida en servidor para pantallas y API; no se eligen roles desde una página pública.
- Solo un `ADMIN` puede invitar en `/usuarios/nuevo`. La invitación crea una cuenta inactiva, guarda un token con vencimiento de 24 h y envía un enlace de verificación. La contraseña la elige el invitado en `/invitacion`; nunca se envía por correo.
- "Olvidé mi contraseña" está en `/recuperar`. La respuesta es idéntica exista o no la cuenta. El token de recuperación vence en 30 minutos.
- Un `GET` con token solo valida y muestra el formulario; el token se consume únicamente con el `POST` de confirmación. Emitir un token nuevo invalida los anteriores del mismo propósito. Los tokens se guardan como hash y no se registran en logs ni respuestas.
- Cambiar la contraseña (perfil o recuperación) expira todas las sesiones anteriores del usuario; el siguiente uso de una sesión expirada redirige a `/login?logout`. El aviso de cambio se envía si hay correo registrado.
- No se puede desactivar al último administrador activo. Las cuentas existentes antes de V7 conservaron su contraseña y quedaron verificadas para no bloquear el acceso local.
- Límites por ventana: 5 intentos de login por cuenta y 15 por IP en 15 min; 3 solicitudes de recuperación por cuenta y 10 por IP en 30 min; 5 invitaciones por cuenta y 20 por IP en 60 min. El reenvío comparte el límite de invitación. La IP se toma de `getRemoteAddr()` salvo `TRUST_PROXY_HEADERS=true`.
- CSRF activo en formularios y peticiones de cambio. El logout exige `POST /logout`.
- Las pantallas sin sesión redirigen a `/login`; `/api/**` sin sesión responde `401` JSON y con rol insuficiente `403` JSON. `/recuperar`, `/recuperar/restablecer` y `/invitacion` son públicas y no filtran existencia de cuentas.
- `/actuator/health` es público y no expone componentes ni detalles.
- La cookie `JSESSIONID` es `HttpOnly`, `SameSite=Lax` y solo `Secure` cuando `COOKIE_SECURE=true`.
- La auditoría guarda el usuario autenticado en ventas y movimientos nuevos. Los registros anteriores conservan `NULL`.
- Los enlaces se construyen desde `APP_BASE_URL`. Los fallos de Brevo se registran de forma genérica: la aplicación no afirma que un correo fue entregado, solo que la solicitud fue aceptada por la API.

## Pantallas

| Ruta | Contenido |
|---|---|
| `/` o `/dashboard` | Ventas del día, ingresos por productos y servicios, stock bajo, últimos movimientos y ventas recientes. |
| `/ventas`, `/ventas/nueva`, `/ventas/{id}` | Historial inmutable, formulario con total provisional y detalle histórico. |
| `/inventario` | Alta, edición, búsqueda, filtros, entradas, ajustes e historial por producto. |
| `/servicios` | Alta, búsqueda, edición, activación y desactivación de servicios. |
| `/movimientos` | Entradas, ajustes y salidas con filtros por producto, tipo y fechas, y paginación. |
| `/usuarios`, `/usuarios/nuevo` | Solo `ADMIN`: listado, invitación, reenvío y activación/desactivación. |
| `/perfil` | Nombre visible y cambio de contraseña comprobando la actual. |
| `/recuperar`, `/recuperar/restablecer` | Solicitud y confirmación de recuperación, sin sesión. |
| `/invitacion` | Verificación de correo y establecimiento de contraseña, sin sesión. |
| `/modulos` | Índice de los módulos disponibles. |

El total mostrado en el formulario de ventas es orientativo: el servidor ignora importes del navegador, toma precios vigentes, valida existencias y guarda los subtotales calculados. Las ventas confirmadas no se editan ni se anulan en esta versión. Los productos referenciados se desactivan, no se borran.

## API REST

Base: `http://localhost:8080`. Todas las rutas salvo `/actuator/health` requieren sesión. Lecturas: `ADMIN` u `OPERADOR`. Escrituras de catálogo (`/api/productos`, `/api/servicios`, entradas y ajustes) y `/api/usuarios/**`: solo `ADMIN`. `/api/ventas/**`: `ADMIN` u `OPERADOR`. `/api/dashboard` y `/api/movimientos`: `ADMIN` u `OPERADOR`.

### Inventario

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/productos` | Listar. Filtros: `q`, `activo`, `stockBajo` |
| `POST` | `/api/productos` | Crear producto (stock inicial 0) |
| `GET` | `/api/productos/{id}` | Consultar uno |
| `PUT` | `/api/productos/{id}` | Editar datos; no acepta `stockActual` ni `activo` |
| `PATCH` | `/api/productos/{id}/activo` | Activar o desactivar |
| `POST` | `/api/productos/{id}/entradas` | Registrar entrada |
| `POST` | `/api/productos/{id}/ajustes` | Registrar ajuste con motivo |
| `GET` | `/api/productos/{id}/movimientos` | Historial del producto |

### Servicios

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/servicios` | Listar. Filtros: `q`, `activo` |
| `POST` | `/api/servicios` | Crear servicio |
| `GET` | `/api/servicios/{id}` | Consultar uno |
| `PUT` | `/api/servicios/{id}` | Editar nombre, descripción, precio y estado |
| `PATCH` | `/api/servicios/{id}/activo` | Activar o desactivar |

Ejemplo de servicio:

```json
{
  "nombre": "Instalación de café",
  "descripcion": "Visita de una hora",
  "precio": 25.00,
  "activo": true
}
```

### Ventas

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/ventas` | Confirmar venta |
| `GET` | `/api/ventas` | Listar historial |
| `GET` | `/api/ventas/{id}` | Consultar detalle histórico |

Ejemplo de venta mixta:

```json
{
  "claveIdempotencia": "0b7f8f8e-8e8d-4b7c-9f5d-2f5b8e1a1234",
  "items": [
    { "tipo": "PRODUCTO", "referenciaId": 1, "cantidad": 2 },
    { "tipo": "SERVICIO", "referenciaId": 1, "cantidad": 1 }
  ]
}
```

La confirmación valida UUID, cantidades enteras positivas, consolidación de repetidos, productos y servicios activos, y stock suficiente. Bloquea la clave de idempotencia con `pg_advisory_xact_lock` y los productos con `SELECT ... FOR UPDATE` en orden ascendente de id. En una sola transacción guarda la venta, el detalle con precios históricos, la salida de inventario y el evento `VentaRegistrada` en outbox. Reenviar la misma clave con el mismo contenido devuelve la venta original; reutilizarla con otros datos produce conflicto.

### Movimientos y dashboard

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/movimientos` | Filtros: `productoId`, `tipo`, `desde`, `hasta`, `page`, `size` |
| `GET` | `/api/dashboard` | Resumen operativo |
| `GET` | `/actuator/health` | Estado público sin detalles |

Los movimientos no se editan ni se borran. Cada salida muestra producto, tipo, cantidades anterior/resultante, fecha local, motivo, usuario y venta relacionada.

## Outbox acordado con Lewis

`outbox_eventos` guarda el evento sin publicarlo. Jean solo lo persiste; Lewis implementa el publicador y los consumidores.

- Exchange: `ultima.events` (topic).
- Clave de enrutamiento: `venta.registrada`.
- Colas durables previstas: `ultima.comprobantes` y `ultima.dashboard`.
- Evento: `VentaRegistrada`, `schemaVersion` 1.
- Payload: `eventId` UUID, `eventType`, `schemaVersion`, `occurredAt` UTC, `ventaId`, `total`, `totalProductos`, `totalServicios` y `moneda` `COP`; importes como cadenas decimales.
- Estados: `PENDIENTE`, `PUBLICADO`, `FALLIDO`.

Las migraciones de Jean son V1 a V8: V1–V6 comerciales y V7–V8 de usuarios/correo. Lewis debe usar V9 o superior y coordinar el número antes de aplicar. V1 a V6 no se modificaron.

El dashboard usa actualmente `ResumenDashboardTablasService`, habilitado por `ultimashop.dashboard.fuente=tablas`. Es un proveedor reemplazable: consulta tablas operativas, no simula eventos procesados ni comprobantes.

## Estructura

```text
api/
├── .env.example
├── pom.xml
├── mvnw, mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/ultimashop/
    │   │   ├── comun/                 # auditoría y zona horaria
    │   │   ├── dashboard/             # resumen operativo y controladores
    │   │   ├── inventario/            # productos y movimientos base
    │   │   ├── movimientos/           # consulta paginada de movimientos
    │   │   ├── seguridad/             # usuario, roles, tokens, correo y configuración
    │   │   ├── servicios/             # catálogo de servicios
    │   │   ├── ventas/                # ventas, detalle y outbox
    │   │   └── web/HomeController.java
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/             # layout, login, seis módulos y seguridad/
    │       ├── static/css/            # tokens morados y estilos locales
    │       ├── static/js/app.js
    │       └── db/migration/          # V1 a V8
    └── test/java/com/ultimashop/
        ├── inventario/
        ├── seguridad/
        ├── ventas/
        └── web/
```

## Pruebas automatizadas

```powershell
.\mvnw.cmd -B clean verify
```

Las pruebas usan PostgreSQL 16-alpine de Testcontainers, nunca la base de desarrollo. El correo se prueba contra un servidor HTTP local simulado, sin contactar a Brevo. Cubren inventario, servicios, ventas mixtas, idempotencia, concurrencia, stock insuficiente, inactivos, transaccionalidad, outbox, movimientos, dashboard, roles, permisos, CSRF, login, logout, respuestas 401/403, health público, invitaciones, verificación de correo, expiración y reutilización de tokens, consumo concurrente, recuperación, revocación de sesiones, límite de intentos y errores de Brevo.

Resultado real de la ejecución final (2026-11-19): **BUILD SUCCESS**, 39 pruebas, 0 fallos y 0 errores. Flyway validó y aplicó las ocho migraciones (V1–V8) en cada base efímera, terminando en `v8`.

Desglose: `InventarioIntegracionTest` 11, `SeguridadIntegracionTest` 9, `UsuariosCorreoIntegracionTest` 12, `ComercioIntegracionTest` 6 y `HomeControllerTest` 1. Quedan avisos no bloqueantes de deprecación por `AntPathRequestMatcher` y la advertencia de Mockito/JDK 21 por carga dinámica de agente.

## Verificación en vivo (2026-11-19)

Se ejecutó un recorrido ficticio contra la API local sobre PostgreSQL (`ultimashop-local-dev-postgres`), sin borrar los datos preexistentes:

- Login con sesión y CSRF correctos; `/dashboard` respondió 200.
- Producto ficticio `DEMO-MIX-20261119105430` (id 2, precio 12.50, stock mínimo 2) y servicio `SERV-000001` (precio 30.00).
- Entrada de stock `+10`, venta mixta id 1 con clave de idempotencia `20e852eb-4bba-4167-bc2b-d1424f27cd00`: 3 productos ($ 37.50) y 2 servicios ($ 60.00), total $ 97.50 calculado en servidor.
- Reenviar la misma clave devolvió la venta id 1; reutilizarla con otro contenido devolvió 409.
- Stock final 7, una salida `SALIDA` de `-3` asociada a la venta 1 y al usuario `Admin Demo`.
- Dashboard: 1 venta en el día, $ 97.50 de ingresos ($ 37.50 productos, $ 60.00 servicios) y movimientos recientes reales.
- Outbox: una fila `VentaRegistrada`, `schemaVersion` 1, `ultima.events` / `venta.registrada`, estado `PENDIENTE`, con `moneda` `COP` e importes como cadenas (`"97.50"`, `"37.50"`, `"60.00"`). `eventId` `085a20ef-6ccb-4bb3-89e8-f6b8c406ce55`. No se publicó ni se simuló consumo.
- Pantallas `/dashboard`, `/ventas`, `/ventas/1`, `/ventas/nueva`, `/servicios`, `/movimientos`, `/inventario` y `/modulos`: 200 con datos reales. API sin sesión: 401 JSON; pantalla sin sesión: redirección a `/login`; logout: redirección a `/login`; `/actuator/health`: 200 con `{"status":"UP"}`.
- Revisión en navegador: sin errores en consola ni recursos remotos (solo CSS/JS locales y un SVG embebido en `data:`). Diseño comprobado a 1000 px y a 390 px: menú lateral oculto con hamburguesa, tablas en tarjetas y cuadrícula de indicadores de dos columnas. No se capturaron capturas de pantalla porque la ventana del escritorio no estaba visible; la comprobación fue de DOM, estilos calculados y recursos cargados.

Tras la actividad de usuarios y correo se repitió una comprobación breve contra la misma base local, sin borrar datos y con `MAIL_ENABLED=false`: Flyway aplicó V7 y V8 sobre `v6` y terminó en `v8`; el inicializador informó que la contraseña existente no se modificó; el login con `admin` funcionó; `/usuarios` mostró la cuenta como `Administrador`; `/perfil`, `/recuperar` y `/dashboard` respondieron sin errores de consola; el dashboard siguió mostrando la venta de $ 97.50 y `DEMO-CAFE`. No se enviaron correos reales ni se crearon cuentas ficticias locales.

Los datos anteriores (`DEMO-CAFE`, id 1, inactivo con stock 4) se conservaron.

## Alcance y pendientes

Fuera de alcance inicial: pagos, facturación electrónica legal, agendas, múltiples negocios, anulaciones, publicación RabbitMQ, comprobantes del worker, resumen asíncrono y despliegue. No se crearon commits, push ni despliegues.
