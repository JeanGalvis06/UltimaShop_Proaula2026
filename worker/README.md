# Worker de Ultima Shop

Proceso Java 21 / Spring Boot 3.5.16 con Spring AMQP y JDBC. La API aplica Flyway V9 y el worker espera su salud antes de comenzar.

## Flujo
1. El publicador selecciona outbox pendiente con bloqueo y SKIP LOCKED, publica mensajes persistentes y comprueba confirmación y retorno.
2. Exchange topic `ultima.events`, clave `venta.registrada`.
3. `ultima.comprobantes` genera documento interno por venta; `ultima.dashboard` actualiza resumen por día de America/Bogota.
4. Cada consumidor registra `(consumidor,event_id)` junto al efecto en una transacción. Las proyecciones también protegen `venta_id` de duplicados. No se vuelve a descontar stock.
5. Un fallo de publicación conserva el evento pendiente con reintento a 15 segundos. Cada consumidor reintenta tres veces antes de pasar a su cola `.errores` mediante `ultima.errores`.

El consumidor valida el contrato frente al evento original y toma importes de la venta histórica. Un comprobante es académico, no facturación electrónica legal. Las tablas son compartidas para simplificar esta demostración; no se afirma independencia de base de datos entre microservicios.

## Configuración
DB_URL, DB_USER, DB_PASSWORD, RABBIT_HOST, RABBIT_PORT, RABBIT_USER, RABBIT_PASSWORD. SERVER_PORT por defecto 8082. PUBLICADOR_ENABLED y CONSUMIDORES_ENABLED permiten demostraciones controladas. No hay secretos en este repositorio.

## Pruebas
Desde worker: `../api/mvnw.cmd -B -ntp clean verify` (PowerShell con Java 21), o `bash ../api/mvnw -B -ntp clean verify` en Linux.

Seis pruebas con PostgreSQL 16 y RabbitMQ 4 reales en Testcontainers: publicación en dos colas, duplicados, concurrencia, recuperación del consumidor, DLQ y mensajes no enrutables. Usa las migraciones de `../api/src/main/resources/db/migration` en base efímera; nunca vacía desarrollo.
