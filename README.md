# Ecommerce Backend

Backend Spring Boot multi-modulo para el simulador ecommerce.

## Modulos

- `common-web`: utilidades web comunes, respuestas estandar, OpenAPI y seguridad interna.
- `identity-service`: registro, login, JWT y usuario actual.
- `catalog-service`: productos, categorias, marcas e inventario.
- `commerce-service`: carrito, checkout y ordenes.
- `api-gateway`: punto unico de entrada, validacion JWT, scopes y Swagger agregado.

## Seguridad

El acceso externo debe pasar por `api-gateway`.

Los microservicios internos requieren `X-Internal-Gateway-Token`; si se llaman directo sin ese token responden `404`, incluyendo health, Swagger y endpoints de negocio.

En Kubernetes, `INTERNAL_GATEWAY_TOKEN` debe venir del Secret `ecommerce-internal-gateway`.

## Ejecutar local en Windows

El entorno local no necesita Docker, WSL ni una distribucion Linux. Requiere:

- JDK 21 o superior.
- Maven 3.9 o superior.
- PostgreSQL instalado como servicio de Windows y escuchando en `localhost:5432`.

Los perfiles `local` conservan el puerto `8080` de Kubernetes como valor base y
asignan puertos distintos cuando los cuatro procesos comparten Windows:

| Aplicacion | Puerto local | Base de datos |
| --- | ---: | --- |
| API Gateway | 8080 | No usa base de datos |
| Identity | 8081 | `identity_db` |
| Catalog | 8082 | `catalogdb` |
| Commerce | 8083 | `commercedb` |

### Preparar PostgreSQL

La primera vez, ejecuta desde PowerShell:

```powershell
.\scripts\windows\setup-postgres.cmd
```

El script solicita la clave del administrador `postgres` sin guardarla y prepara
de forma idempotente las tres bases y sus usuarios de aplicacion.

### Iniciar y detener todo el backend

```powershell
.\scripts\windows\local.cmd start
```

El comando compila el reactor, inicia los cuatro JAR con el perfil `local`,
comprueba PostgreSQL y espera a que todos los health checks respondan `UP`.
Los procesos se ejecutan en segundo plano y sus logs quedan en
`%LOCALAPPDATA%\EcommerceSimulationBack\logs`.

```powershell
.\scripts\windows\local.cmd status
.\scripts\windows\local.cmd stop
```

Si los JAR ya estan compilados, puede omitirse el build:

```powershell
.\scripts\windows\local.cmd start -SkipBuild
```

El tiempo de espera predeterminado es de 120 segundos. Puede ampliarse en un
primer arranque especialmente lento:

```powershell
.\scripts\windows\local.cmd start -StartupTimeoutSeconds 180
```

`stop` cierra solamente los cuatro procesos Java; PostgreSQL permanece activo.
Las variables de entorno existentes (`JWT_SECRET`, `INTERNAL_GATEWAY_TOKEN` y
las variables de datasource) siguen pudiendo reemplazar los valores locales.

Docker Compose y los manifiestos de Kubernetes se conservan para despliegues en
contenedores o clusters. No son necesarios para desarrollar y probar en Windows.

## Rutas Principales

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Identity: `http://localhost:8080/api/auth/**`, `http://localhost:8080/api/users/**`
- Catalog: `http://localhost:8080/api/products/**`, `http://localhost:8080/api/categories/**`, `http://localhost:8080/api/brands/**`
- Commerce: `http://localhost:8080/api/cart/**`, `http://localhost:8080/api/checkout/**`, `http://localhost:8080/api/orders/**`

## Probar

```powershell
mvn.cmd test
```
