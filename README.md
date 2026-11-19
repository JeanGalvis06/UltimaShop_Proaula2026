# Ultima Shop

Gestor de inventario, ventas y servicios para emprendedores. Proyecto académico de Proaula y DevOps en Java 21, Spring Boot, Thymeleaf y PostgreSQL.

## Módulos
- Dashboard con consultas comerciales provisionales.
- Inventario: productos, entradas, ajustes y existencias.
- Servicios de precio fijo.
- Ventas de productos y servicios, con control de stock e idempotencia.
- Historial de movimientos y autenticación con sesión.

Una venta registra sus detalles, stock, movimientos y evento outbox en una transacción. El worker RabbitMQ y el resumen asíncrono todavía requieren integración y verificación. No se presentan como completados.

## Estructura
| Carpeta | Contenido |
|---|---|
| `api/` | Aplicación Java, interfaz, migraciones y pruebas |
| `local-dev/` | PostgreSQL local, plantillas de configuración e instrucciones |
| `infra/` | Dockerfile, overlay API y ejemplo de Render |
| `.github/workflows/` | Compilación, pruebas y construcción de imagen |
| `docs/` | Estado de entrega y comprobaciones |

La referencia visual `ultima-store`, secretos, logs y archivos compilados quedan excluidos del repositorio.

## Empezar en Windows
1. Tener JDK 21 y Docker Desktop funcionando.
2. Seguir `local-dev/README.md` para crear la configuración privada y levantar PostgreSQL con volumen persistente.
3. Seguir `api/README.md` para exportar variables y ejecutar con Maven Wrapper. Los archivos `.env` no se cargan automáticamente por existir.
4. Para compilar y probar desde `api/`: `./mvnw.cmd -B -ntp clean verify`. Las pruebas PostgreSQL usan Testcontainers y necesitan Docker.

No hay contraseña predeterminada publicada: el primer administrador se configura por variables privadas. Brevo permanece desactivado. Usar datos ficticios para la demostración.

## Docker y CI
Desde la raíz: `docker build -f infra/Dockerfile.api -t ultimashop-api:local .`.

El Dockerfile compila el JAR omitiendo pruebas de integración dentro del build; esas pruebas se ejecutan antes, en el host o en GitHub Actions. Ver `docs/ENTREGA_DEVOPS.md` antes de activar el overlay sobre la base existente.

GitHub Actions verifica Java, ejecuta las pruebas y construye la imagen en pushes y solicitudes de cambios a main. La publicación GHCR es manual y no está habilitada por un push normal.

## Estado y límites
Verificación local del 19 de noviembre de 2026: 39 pruebas correctas en una copia aislada; imagen construida y arrancada contra PostgreSQL temporal, con salud UP y API protegida. Esto no sustituye la ejecución del workflow remoto ni valida cambios posteriores.

Render tiene una plantilla revisable, todavía sin despliegue. GitHub Actions no sustituye Gitea si el parcial exige esa herramienta. No se implementan pagos, facturación electrónica legal, múltiples negocios o anulaciones. Consultar `docs/ENTREGA_DEVOPS.md` para pendientes y pasos de demostración.
