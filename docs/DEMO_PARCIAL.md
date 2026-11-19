# Demostración de Ultima Shop

## Servicios locales
| Servicio | Dirección | Estado comprobado |
|---|---|---|
| API en Docker | http://localhost:8081 | Salud UP, login y pantallas |
| Integración de eventos | http://localhost:8081/integracion | Outbox y proyecciones reales |
| Worker | http://localhost:8082/actuator/health | Salud UP |
| RabbitMQ Management | http://localhost:15672 | Broker persistente y dos consumidores |
| Gitea | http://localhost:3000/jean/UltimaShop | Servicio, administrador y repositorio local |
| PostgreSQL | 127.0.0.1:5432 | Volumen de desarrollo conservado |

El programa Java anterior en 8080 no se detuvo. Para mostrar los servicios integrados usar 8081.

## Credenciales privadas
- Administrador de demostración: `local-dev/.env.demo`.
- RabbitMQ: `RABBIT_USER` / `RABBIT_PASSWORD` en `local-dev/.env`.
- Gitea: `GITEA_ADMIN_USER` / `GITEA_ADMIN_PASSWORD` en `local-dev/.env.gitea`.
Estos archivos están ignorados por Git. Abrirlos solo localmente; no mostrarlos al proyectar ni compartirlos. No existen contraseñas predeterminadas publicadas. Los usuarios anteriores no se modificaron.

## Arranque
Desde la raíz del repositorio:

```powershell
docker compose --env-file local-dev/.env --env-file local-dev/.env.demo -f local-dev/compose.yaml -f infra/compose.api.yaml -f infra/compose.eventos.yaml up -d --build
docker compose --env-file local-dev/.env.gitea -f infra/compose.gitea.yaml -f infra/compose.runner.yaml up -d
```

Los archivos privados deben estar ya configurados. Un clon nuevo requiere crearlos; Compose indica qué variable falta. No ejecutar down -v ni borrar volúmenes. Hay respaldo previo a V9 en `local-dev/backups/`, excluido del repositorio.

El runner monta el motor Docker para probar y construir este repositorio confiable. Jean autorizó ese acceso explícitamente. No permitir workflows externos/no confiables. Para apagarlo al terminar: `docker compose --env-file local-dev/.env.gitea -f infra/compose.gitea.yaml -f infra/compose.runner.yaml stop runner`.

## Recorrido del parcial
1. Entrar en 8081 y mostrar módulos.
2. Crear producto con precio 10.000 y servicio de 5.000 COP; ingresar 10 unidades.
3. Vender dos productos y un servicio: total 25.000 COP, stock 8 y salida de dos unidades.
4. Abrir el comprobante desde el detalle de la venta. Mostrar `/integracion` y el resumen del dashboard.
5. En Docker Desktop parar solo el worker y registrar otra venta. Debe confirmarse y mostrar comprobante pendiente.
6. Iniciar worker. Actualizar integración/comprobante: procesa la venta pendiente.
7. Mostrar las dos colas y sus consumidores en RabbitMQ Management; no enseñar credenciales.
8. Mostrar commit, pruebas y construcción en GitHub Actions y, cuando termine, en Gitea Actions.

## Evidencia ejecutada el 19 de noviembre de 2026
- API: 39 pruebas, 0 fallos/errores, con migración V9 en copia aislada.
- Worker: 6 pruebas, 0 fallos/errores, con RabbitMQ/PostgreSQL reales y efímeros.
- Venta de demostración #2: producto + servicio, 25.000 COP, stock 8, comprobante US-00000002. Reintentar la misma venta devolvió la misma operación.
- Worker detenido: venta #3 confirmada y comprobante pendiente. Al recuperarlo generó US-00000003; stock final del producto 7.
- Reentrega real del evento por RabbitMQ: no duplicó comprobantes ni ventas proyectadas.
- Pantallas de comprobante e integración HTTP 200. Fuente del dashboard `worker`, outbox sin pendientes al finalizar la comprobación.
- API, worker, broker y PostgreSQL saludables y persistentes; base existente no borrada.

## Límites y pendientes
Render está pendiente de crear/configurar PostgreSQL y servicio web. Su plantilla por sí sola no es un despliegue. El despliegue cloud y la publicación de imágenes se verifican por separado. Brevo permanece desactivado. El pipeline Gitea se considera ejecutado únicamente cuando su interfaz muestre resultado real, no por existir un YAML.

`diagramas-originales/` conserva los PUML entregados como referencia, no como evidencia. Los diagramas genéricos de inventario/facturación/notificaciones no describen literalmente los dos consumidores de esta implementación; el stock se descuenta en la transacción de la API.
