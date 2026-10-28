# %serviceSlug%

Backend service (Java 25 LTS, Micronaut 5.2, Maven) managed by MFE Orchestrator.

## Endpoints

Port `8080` (override with `PORT`). Docs and probes need no auth.

| Purpose      | Method | Path                      |
| ------------ | ------ | ------------------------- |
| Liveness     | GET    | `/health/liveness`        |
| Readiness    | GET    | `/health/readiness`       |
| Docs UI      | GET    | `/swagger-ui/index.html`  |
| OpenAPI spec | GET    | `/swagger/openapi.json`   |
| Hello        | GET    | `/api/hello`              |

Probes answer `{"status":"UP"}`; readiness answers `503` with `DOWN` during graceful shutdown. The OpenAPI spec and the Swagger UI are generated at build time from the controller annotations (`openapi.properties` configures them).

## Run locally

Requires JDK 25. The Maven Wrapper downloads Maven on first use.

```bash
./mvnw mn:run                   # http://localhost:8080
curl localhost:8080/health/liveness
open http://localhost:8080/swagger-ui/index.html
```

## Configuration

Environment variables only:

| Variable            | Default                           | Description                                                          |
| ------------------- | --------------------------------- | -------------------------------------------------------------------- |
| `PORT`              | `8080`                            | HTTP port                                                            |
| `SHUTDOWN_TIMEOUT`  | `20s`                             | On SIGTERM, readiness reports `DOWN` and in-flight requests get this long to finish |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` (image) | JVM flags; the heap follows the container memory limit               |

Any Micronaut property can also be set from the environment (e.g. `MICRONAUT_SERVER_PORT`). Keep `SHUTDOWN_TIMEOUT` below the Kubernetes `terminationGracePeriodSeconds` (30s by default).

## Test and build

```bash
./mvnw verify                   # tests + target/service.jar (executable shaded jar)
java -jar target/service.jar
```

## Docker

```bash
docker build -t %serviceSlug% .
docker run --rm -p 8080:8080 %serviceSlug%
```

Multi-stage JVM build: `eclipse-temurin:25-jdk-alpine` builds the jar, `eclipse-temurin:25-jre-alpine` runs it as the non-root user `10001`. The `HEALTHCHECK` calls `/health/liveness` with the BusyBox `wget` of the alpine base (no curl in the image).

Kubernetes probes:

```yaml
livenessProbe:
  httpGet: { path: /health/liveness, port: 8080 }
readinessProbe:
  httpGet: { path: /health/readiness, port: 8080 }
```

## Dependency updates

`.github/dependabot.yml` opens weekly pull requests for the Maven dependencies and plugins, the Docker base images and the GitHub Actions, with minor and patch updates grouped into one PR per ecosystem. Majors of the `eclipse-temurin` base images are ignored: move to the next Java LTS on purpose (Dockerfile, CI `java-version`, `maven.compiler.release` in `pom.xml` together). GitLab and Azure DevOps do not read this file: use Renovate there if you need the same.

## Releases

1. Create a new version of the service in the MFE Orchestrator console (this creates a git tag on the repository).
2. The tag triggers CI: tests (`./mvnw -B verify`), image build and push to the container registry, then the image is registered on the console (`POST /api/backends/by-slug/%serviceSlug%/versions/<tag>` with `api-key`).
3. The version shows up in the console with its image reference and digest.

The deploy secret `FLIGHTROOM_API_KEY` is injected in the repository by the console.

- GitHub Actions: `.github/workflows/release.yml` pushes to `ghcr.io/<owner>/<repo>` using `GITHUB_TOKEN`. Optional repo variable `MICROFRONTEND_ORCHESTRATOR_DOMAIN` overrides the console URL.
- GitLab CI: `.gitlab-ci.yml` pushes to the project's container registry.
- Azure DevOps: `azure-pipelines.yml` needs two pipeline variables:
  - `MFE_ORCHESTRATOR_REGISTRY_CONNECTION`: name of the Docker registry service connection;
  - `MFE_ORCHESTRATOR_IMAGE_REPOSITORY`: repository path in the registry (e.g. `my-org/%serviceSlug%`).
  Optionally `MFE_ORCHESTRATOR_REGISTRY_HOST` (e.g. `myregistry.azurecr.io`) so the registered image reference is fully qualified, and `MICROFRONTEND_ORCHESTRATOR_DOMAIN` to override the console URL. The secret comes from the variable group `MFE_ORCHESTRATOR_SECRETS`. The digest is not sent from Azure (optional field).

## Deploys

The orchestrator does not deploy the service: your own deploy pipeline does. So that the console can show which version runs in each environment (domain page, deployed versions matrix), have that pipeline report every successful deploy. Set `ENV_SLUG` (the slug of the environment in the console, e.g. `DEV`, `PROD`), `VERSION`, `IMAGE` and `COMMIT_SHA`, with `FLIGHTROOM_API_KEY` holding the deploy secret:

```bash
DOMAIN="${MICROFRONTEND_ORCHESTRATOR_DOMAIN:-%domain%}"
curl -fsS -X POST "${DOMAIN%/}/api/backends/by-slug/%serviceSlug%/environments/${ENV_SLUG}/deployments" \
  -H "api-key: ${FLIGHTROOM_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"version\":\"${VERSION}\",\"image\":\"${IMAGE}\",\"commitSha\":\"${COMMIT_SHA}\"}"
```

- `ENV_SLUG` is matched regardless of case. When two environments of the project differ only by case (e.g. `prod` and `PROD`), use the exact slug: any other spelling is refused with a 409.
- `version` is required (letters, digits, dots, dashes and underscores, at most 128); `image` (at most 512 characters) and `commitSha` (at most 64) are optional. The version does not have to be one the release CI registered.
- The API key needs the MANAGER role, as the deploy secret has. Every report is kept: the latest one is the version shown for that environment.
