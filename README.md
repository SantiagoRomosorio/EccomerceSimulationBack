# Hexagonal Spring Boot

Backend Spring Boot base usando arquitectura hexagonal.

Incluye:

- Spring Web
- Spring Data JPA
- Spring Security
- JWT con `jjwt`
- Validation
- DevTools
- Lombok
- MapStruct
- OpenAPI/Swagger
- Actuator
- PostgreSQL para desarrollo local
- Driver PostgreSQL para usar una base real

La idea principal es separar el negocio de los detalles externos:

- `domain`: modelo del negocio, reglas propias del sistema y excepciones de dominio.
- `application`: casos de uso, puertos de entrada y puertos de salida.
- `adapter`: controladores, persistencia, JWT, SQL puro, mappers y clientes externos.
- `config`: configuracion tecnica de Spring Boot, seguridad, beans e inyeccion de dependencias.

En esta arquitectura, el dominio no debe depender de Spring, JPA, JWT, HTTP ni bases de datos.

## Ejecutar

```bash
mvn spring-boot:run
```

## Probar

```bash
mvn test
```

Endpoints utiles al iniciar:

- `GET /api/health`
- `GET /actuator/health`
- `GET /swagger-ui.html`
- `GET /postgres-console`

Nota: en esta maquina no hay Maven instalado, por eso no se ejecuto la compilacion localmente.
# AtlasElectoral
# AtlasElectoral
# AtlasElectoral
# AtlasElectoral
