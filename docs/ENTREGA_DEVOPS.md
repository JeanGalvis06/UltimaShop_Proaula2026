# Entrega DevOps de Ultima Shop

## Alcance y estado
Preparación de Docker y GitHub Actions para la API existente, sin cambiar api/src ni api/pom.xml. RabbitMQ y worker quedan pendientes; este documento no los presenta como integrados. Brevo permanece desactivado.

Repositorio indicado: https://github.com/JeanGalvis06/UltimaShop_Proaula2026 . Al revisar, GitHub lo reportó vacío. No se ha publicado el código por esta preparación. GitHub Actions no demuestra Gitea: si el parcial exige Gitea, esa herramienta conserva su propia implementación/evidencia pendiente.

## Docker sin tocar el volumen actual
El Dockerfile usa Java 21, compila con Maven Wrapper y ejecuta con usuario no root. El build omite pruebas de integración porque necesitan motor Docker: se ejecutan ANTES en el host o en CI. Una imagen construida no acredita que las pruebas pasaron.

Desde la raíz del proyecto, la siguiente combinación reutiliza el Compose y volumen de desarrollo existentes. No ejecutarla hasta terminar una prueba aislada y coordinar con OpenCode. La API en contenedor usaría 8081 para evitar pisar la aplicación actual en 8080:

```powershell
docker compose --env-file local-dev/.env -f local-dev/compose.yaml -f infra/compose.api.yaml config --quiet
docker compose --env-file local-dev/.env -f local-dev/compose.yaml -f infra/compose.api.yaml up -d --build api
curl.exe -f http://localhost:8081/actuator/health
```

El primer archivo debe ser local-dev/compose.yaml: las rutas del overlay dependen de esa ubicación. No cambiar el nombre del proyecto Compose ni crear un PostgreSQL paralelo sobre el mismo volumen. Si el código o las migraciones están cambiando, no arrancar simultáneamente dos versiones distintas de la API contra desarrollo. El overlay no se activa por guardarlo.

Parar solamente la API contenedorizada:

```powershell
docker compose --env-file local-dev/.env -f local-dev/compose.yaml -f infra/compose.api.yaml stop api
```

No usar down -v. No imprimir `compose config` completo: puede revelar credenciales interpoladas; usar `config --quiet`.

## Pipeline
`.github/workflows/ci.yml` verifica Java 21, ejecuta `clean verify` con Testcontainers y construye la imagen. No publica nada en un push normal. Una ejecución manual permite publicar en GHCR desde main si se selecciona expresamente la opción; usa GITHUB_TOKEN temporal, sin guardar un PAT en archivos. Revisar visibilidad y permisos del paquete antes de usarlo fuera de GitHub.

El pipeline aún necesita que el código revisado se publique y se ejecute en GitHub. Su archivo YAML no constituye evidencia de una ejecución. Las acciones y la imagen base deben volver a verificarse y fijarse por SHA/digest antes de una entrega que requiera inmovilidad de dependencias.

## Render pendiente de configuración real
Se puede desplegar una imagen GHCR por SHA o construir desde este Dockerfile. Para la ruta con imagen preconstruida: indicar URL/tag y credenciales de registro si es privado; preparar PostgreSQL remoto y configurar DB_URL con formato JDBC, DB_USER y DB_PASSWORD. No usar localhost ni el hostname postgres de Compose como base remota.

Configurar SERVER_PORT con el valor de PORT que indique Render (o comprobar coincidencia explícita), health path /actuator/health, COOKIE_SECURE=true con HTTPS, APP_BASE_URL con URL pública y MAIL_ENABLED=false. El administrador se inicializa mediante variables privadas siguiendo la aplicación existente; no usar credenciales de demostración en un despliegue público. Confirmar permisos y protección de rutas antes de publicar.

No basta subir imagen al registro: solicitar despliegue de la versión mediante el mecanismo de Render elegido y verificar salud. No se ha contratado, creado ni desplegado un servicio Render con estos archivos. RabbitMQ remoto necesita un broker accesible y persistente cuando se integre.

## Comprobaciones para el parcial
- App comercial: verificar login, productos, servicios, venta y rollback de stock insuficiente.
- Docker: construir imagen y comprobar arranque/salud contra PostgreSQL aislado antes de tocar desarrollo.
- GitHub: primer commit revisado sin secretos, push autorizado y ejecución de Actions.
- Gitea: pendiente si es requisito explícito; GitHub no lo sustituye automáticamente.
- Render: cuenta/recursos/configuración y prueba real pendientes.
- RabbitMQ: integrar publicador/colas/consumidores cuando se reciba su implementación.

Fuentes de configuración: https://github.com/actions/setup-java ; https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven ; https://render.com/docs/docker ; https://render.com/docs/health-checks .

## Verificación real del 19 de noviembre de 2026
- Copia aislada de api/src, pom y Wrapper: `clean verify` terminó BUILD SUCCESS; 39 pruebas, 0 fallos, 0 errores y 0 omitidas. La copia evita bloquear el JAR o los archivos que está usando OpenCode. Su resultado aplica a esa captura del código, no a cambios posteriores.
- `docker build -f infra/Dockerfile.api -t ultimashop-api:revision-local .` terminó correctamente. Imagen disponible localmente; no publicada.
- Imagen ejecutada con PostgreSQL 16 efímero en una red temporal, sin puertos publicados ni acceso al volumen de desarrollo: health UP, login HTTP 200, API de productos sin sesión HTTP 401, usuario de contenedor 10001 y healthcheck Docker healthy. Flyway aplicó 8 migraciones.
- Los dos contenedores y la red exclusivos de esa comprobación fueron retirados al terminar. El contenedor, puertos y volumen existentes de desarrollo no se modificaron.
- Overlay Compose validado con `config --quiet`; no activado contra desarrollo.
- Exclusiones comprobadas mediante Git en un repositorio temporal: .env de API, .env y .env.brevo de local-dev, logs y ultima-store quedan ignorados. No se inicializó Git ni se hizo commit/push en la carpeta del usuario.
- La plantilla `infra/render.api.example.yaml` deja los secretos sin valores y autodespliegue desactivado. Es una ruta alternativa de construcción desde GitHub, no un despliegue ejecutado. No crea PostgreSQL ni RabbitMQ ni fija planes; elegirlos y revisar costos antes de aplicar. Para usar como Blueprint debe seleccionarse su ruta explícitamente o copiarse a render.yaml tras revisión. Documentación: https://render.com/docs/blueprint-spec .
- Pendientes reales: primer envío del código revisado a GitHub, ejecución del workflow en GitHub, configuración/despliegue Render y entrega RabbitMQ. Gitea continúa pendiente si lo exige la guía.
