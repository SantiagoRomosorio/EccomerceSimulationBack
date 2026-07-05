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

## Ejecutar Local

Usa el mismo token interno en los cuatro procesos:

```bash
INTERNAL_GATEWAY_TOKEN=local-internal-gateway-token-change-me SERVER_PORT=8081 POSTGRES_URL=jdbc:postgresql://localhost:5432/identity_db POSTGRES_USER=identity_user POSTGRES_PASSWORD=identity_password mvn -Dmaven.repo.local=.m2/repository -pl identity-service -am spring-boot:run
```

```bash
INTERNAL_GATEWAY_TOKEN=local-internal-gateway-token-change-me SERVER_PORT=8082 CATALOG_DB_URL=jdbc:postgresql://localhost:5432/catalogdb CATALOG_DB_USERNAME=catalog CATALOG_DB_PASSWORD=catalog mvn -Dmaven.repo.local=.m2/repository -pl catalog-service -am spring-boot:run
```

```bash
INTERNAL_GATEWAY_TOKEN=local-internal-gateway-token-change-me SERVER_PORT=8083 COMMERCE_DB_URL=jdbc:postgresql://localhost:5432/commercedb COMMERCE_DB_USERNAME=commerce COMMERCE_DB_PASSWORD=commerce CATALOG_SERVICE_URL=http://localhost:8082 mvn -Dmaven.repo.local=.m2/repository -pl commerce-service -am spring-boot:run
```

```bash
INTERNAL_GATEWAY_TOKEN=local-internal-gateway-token-change-me SPRING_PROFILES_ACTIVE=local mvn -Dmaven.repo.local=.m2/repository -pl api-gateway -am spring-boot:run
```

## Rutas Principales

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Identity: `http://localhost:8080/api/auth/**`, `http://localhost:8080/api/users/**`
- Catalog: `http://localhost:8080/api/products/**`, `http://localhost:8080/api/categories/**`, `http://localhost:8080/api/brands/**`
- Commerce: `http://localhost:8080/api/cart/**`, `http://localhost:8080/api/checkout/**`, `http://localhost:8080/api/orders/**`

## Probar

```bash
mvn -Dmaven.repo.local=.m2/repository -pl api-gateway,identity-service,catalog-service,commerce-service -am test
```
