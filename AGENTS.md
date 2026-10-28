# AGENTS.md - %serviceSlug%

Java 25 LTS + Micronaut 5.2 (Netty HTTP server, Serde Jackson) backend service, built with Maven, packaged as a Docker image (JVM mode, executable jar).

## Layout

- `pom.xml`: `micronaut-parent` BOM, `micronaut-http-server-netty`, `micronaut-management` (health), `micronaut-serde-jackson`, OpenAPI annotation processor.
- `src/main/java/dev/mfeorchestrator/service/Application.java`: entry point and `@OpenAPIDefinition`.
- `src/main/java/dev/mfeorchestrator/service/HelloController.java`: example `GET /api/hello`.
- `src/main/resources/application.properties`: port, health endpoint, graceful shutdown, static resources for the OpenAPI spec and Swagger UI.
- `openapi.properties`: OpenAPI generation (output `openapi.json`, Swagger UI on, other viewers off).
- `src/test/java/...`: `@MicronautTest` tests with the HTTP client.
- `Dockerfile`: multi-stage, non-root user `10001`, healthcheck on `/health/liveness`.
- `mvnw`, `.mvn/wrapper/`: Maven Wrapper. Always use `./mvnw`, never a system `mvn`.

## Endpoints the platform relies on

- `GET /health/liveness` and `GET /health/readiness`: Kubernetes probes.
- `GET /swagger-ui/index.html` (Swagger UI) and `GET /swagger/openapi.json` (OpenAPI spec). Both public, no auth.

## Add an endpoint

Create a `@Controller("/api/...")` class in `dev.mfeorchestrator.service` (or a sub-package) with `@Get`, `@Post`, ... methods. Document it for the OpenAPI spec with `@Tag` on the class and `@Operation(summary = ...)` on each method (`io.swagger.v3.oas.annotations`). Return a record or DTO annotated `@Serdeable` so it is serialized (Micronaut Serde needs it, also in native images) and its schema is generated. Add a `@MicronautTest` test that calls it.

## Add a health check

Implement `io.micronaut.management.health.indicator.HealthIndicator` as a `@Singleton`, annotated `@Liveness` (process health only) or `@Readiness` (gates traffic: database, queue, downstream API) from `io.micronaut.management.health.indicator.annotation`. Never check dependencies in a liveness indicator: a failing liveness probe restarts the pod. The probe endpoints work with no custom indicator.

## Commands

- `./mvnw verify` (tests + jar), `./mvnw mn:run`, `./mvnw test -Dtest=ServiceTest`

## Conventions

- Configuration comes from environment variables only (`${ENV_VAR:default}` in `application.properties`, or `MICRONAUT_*` env vars). Never hardcode secrets or URLs.
- Keep the probes, the Swagger UI and `/swagger/openapi.json` working and unauthenticated. If you add security, permit `/health/**`, `/swagger-ui/**` and `/swagger/**`.
- Listen on `PORT` (default 8080). Keep the `micronaut.lifecycle.graceful-shutdown.*` settings.
- Every new endpoint needs a test. Run `./mvnw verify` before finishing.
- Never edit CI files (`.github/`, `.gitlab-ci.yml`, `azure-pipelines.yml`) or Dockerfile base images unless explicitly asked.
- Dependencies are kept current by Dependabot (`.github/dependabot.yml`, weekly, minor/patch grouped). Add new dependencies at their latest stable version; do not pin old versions without a reason.
