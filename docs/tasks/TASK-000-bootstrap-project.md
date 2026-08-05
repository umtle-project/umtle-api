# TASK-000: Bootstrap project to a Ready-to-Develop state

## Status

Done

## Purpose

Verify and fill in the infrastructure gaps that stood between the current skeleton project and an actual "Ready to Develop" state, before any business feature work starts. No business/domain code is added by this task.

## Background

The project's documentation set (`PRD.md` / `REQUIREMENTS.md` / `DOMAIN_MODEL.md` / `ARCHITECTURE.md` / `DEVELOPMENT.md`) and AI operating structure (`AGENTS.md` / `CLAUDE.md`) were already in place, but `src/` was still the unmodified Spring Initializr skeleton — one context-load test, no datasource wiring, no profile split, and Docker files at the repository root. This task inspects and fixes exactly that gap.

## Related Documents and Requirement IDs

- `docs/ARCHITECTURE.md` §7 기술 스택 — Kotlin/Spring Boot, MySQL, Flyway, Spring Security are the accepted stack; this task does not change it.
- `docs/DEVELOPMENT.md` §9 Testing Workflow, §13 Configuration & Infrastructure Conventions.

## Scope

- Confirm the Gradle build/test/run cycle actually works end to end.
- Wire local development to the existing `compose.yaml` (MySQL via Docker Compose), and confirm Flyway and Spring Data JPA connect to it.
- Make `./gradlew test` runnable against a real MySQL instance (Testcontainers), since previously it failed outright with no datasource configured.
- Split Spring configuration into `application.yml` (common) + `application-<profile>.yml` (profile-specific), per the naming/profile split requested for this task.
- Move Docker-related files under `docker/`.
- Confirm the Actuator health endpoint is exposed and reachable.

## Out of Scope

- Any business/domain feature, entity, or table.
- Authentication/authorization policy (still deferred — see `docs/ARCHITECTURE.md` §8 Deferred Decisions item 3).
- CI pipeline (no workflow file added; the repository is not yet git-initialized).
- `application-prod.yml` exists only as an empty placeholder (see below) — no actual production settings, deployment target, or activation wiring is defined yet.

## What changed

### Build (`build.gradle.kts`)

- Added test-only dependencies to make `@SpringBootTest` runnable against a real database:
  `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter:1.21.4`, `org.testcontainers:mysql:1.21.4`.
- These two Testcontainers module versions are pinned explicitly (not left to `io.spring.dependency-management`) — see "Known issue" below.

### Test infrastructure

- `src/test/kotlin/com/umtle/umtleapi/TestcontainersConfiguration.kt` — `@TestConfiguration` that starts a `mysql:8.4` Testcontainers instance and registers it via `@ServiceConnection`, so `@SpringBootTest` gets a real datasource without any manual JDBC URL configuration.
- `UmtleApiApplicationTests.kt` — now `@Import(TestcontainersConfiguration::class)`.
- `ActuatorHealthTests.kt` (new) — `@SpringBootTest` + `@AutoConfigureMockMvc`, asserts `GET /actuator/health` returns `200` with `status: UP`.
- `src/test/resources/application.yml` (new) — sets `spring.profiles.active: test`, so all tests run under the `test` profile without needing `@ActiveProfiles` on every class.

### Configuration profile split

- `src/main/resources/application.yaml` → renamed to `application.yml`. Holds only what's genuinely common: app name, `spring.profiles.active: local` (which profile to use by default), and Actuator health exposure (`management.endpoints.web.exposure.include: health`, `show-details: when-authorized`) — kept common because the health endpoint needs to work identically in every environment.
- `src/main/resources/application-local.yml` — local profile. Holds `spring.docker.compose.file: docker/compose.yaml` (moved out of the common file — it's a local-dev-only concern). No datasource settings: Docker Compose's service connection wires the datasource automatically once that compose file's `mysql` service is running. Confirmed the profile-specific property is still picked up correctly before `DockerComposeLifecycleManager` needs it (`bootRun` log shows `The following 1 profile is active: "local"` immediately followed by `Using Docker Compose file .../docker/compose.yaml`).
- `src/main/resources/application-test.yml` — test profile (activated via `src/test/resources/application.yml`). No overrides needed: Testcontainers service connection wires the datasource automatically.
- `src/main/resources/application-prod.yml` (new) — empty placeholder only. Not activated anywhere, no settings defined; production configuration is intentionally deferred until a real deployment target exists (see `docs/ARCHITECTURE.md` § Deferred Decisions).

### Docker

- `compose.yaml` moved to `docker/compose.yaml`.
- Added `name: umtle-api` to `docker/compose.yaml`. Without it, Docker Compose derives the project name from the containing directory (`docker`), which collides with any other project on the same machine that also names its compose folder `docker` — containers would show up as `docker-mysql-1` instead of `umtle-api-mysql-1`. Confirmed this by running `bootRun` before and after adding `name:`.
- Credentials (`MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`) are hardcoded directly in `docker/compose.yaml`, not sourced from `.env` — this file is local-only (never deployed, never holds anything beyond a throwaway dev DB), so a `.env` indirection added no value. `MYSQL_DATABASE`/`MYSQL_USER` are set to `umtle_local` to make clear this is the local-dev database, distinct from any future environment's database.
- Added a fixed host port (`3306:3306`) and a named volume (`mysql-local-data:/var/lib/mysql`) so local data survives container recreation. Trade-off: this will fail to start if something else on the machine is already bound to `3306` (a system-wide MySQL install, another project's compose stack, etc.) — acceptable for a solo-dev setup, flagged here in case it becomes an issue later.
- A root-level `.env` / `.env.example` were introduced and then removed from this file's configuration. Since nothing in the project reads `.env`, `.env.example` was removed as dead weight before the first commit (§ pre-commit review below). `.gitignore` still ignores `.env` / `.env.*` as a standing rule for whenever a real need appears.

### Flyway

- `flyway-mysql` and `spring-boot-starter-flyway` were already on the classpath. No migration files exist yet because no domain schema has been defined.
- Added `src/main/resources/db/migration/README.md` documenting the `V<version>__<description>.sql` naming convention, so the first real migration has an obvious home when the first domain/table is introduced.
- Verified: on `bootRun`, Flyway connects, finds zero migrations, creates `flyway_schema_history`, and does not fail (`DbValidate: Successfully validated 0 migrations`).

### Documentation

- `docs/DEVELOPMENT.md` §9 Testing Workflow — updated the stale "only one test exists" note, and added that `@SpringBootTest`-based tests need Docker running locally (Testcontainers).
- `docs/DEVELOPMENT.md` §13 (new) Configuration & Infrastructure Conventions — documents the `application-<profile>.yml` naming rule and the `docker/` directory convention.

## Known issue — Testcontainers module versions

`spring-boot-dependencies:4.1.0` declares `testcontainers.version=2.0.5` and imports `testcontainers-bom:2.0.5` as a nested BOM. `io.spring.dependency-management` does not follow that nested import, so unversioned `org.testcontainers:junit-jupiter` / `org.testcontainers:mysql` fail to resolve. Separately, `1.21.4` is the highest version actually published for those two specific modules on Maven Central at the time of this task — `2.0.5` does not exist for them (only the core `testcontainers` artifact is at `2.0.5`). Both versions were pinned explicitly to `1.21.4` to unblock this. If a future `spring-boot-dependencies` upgrade changes this, re-check via `./gradlew dependencyInsight --configuration testCompileClasspath --dependency org.testcontainers`.

Also note: `TestRestTemplate` moved to `org.springframework.boot.resttestclient.TestRestTemplate` in Spring Boot 4.1, and pulling it in triggered a `NoClassDefFoundError` (`org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder`) that could not be resolved with the dependencies currently on the classpath. `ActuatorHealthTests` uses `MockMvc` instead, which avoided the issue entirely and needs no extra dependency.

## Verification performed

- `./gradlew clean build` — succeeds (compile, `bootJar`, `test`).
- `./gradlew clean test` — succeeds with Docker running (`contextLoads`, `healthEndpointReportsUp`), both against a Testcontainers MySQL instance under the `test` profile.
- `./gradlew bootRun` — starts successfully: Docker Compose brings up `mysql:8.4` (as `umtle-api-mysql-1`), Flyway validates/creates the schema history table, JPA/Hibernate connects, Tomcat starts on port 8080.
- `GET http://localhost:8080/actuator/health` → `200 {"status":"UP","groups":["liveness","readiness"]}`, confirmed against the running app (not just the test slice).
- App and Docker Compose containers shut down cleanly after each run (`Exited (0)`).
- Ran `./gradlew clean test` and `bootRun` twice — once before, once after the `docker/` relocation and `.yml` rename — to confirm the change didn't regress anything.

## Acceptance Criteria

- `./gradlew build` and `./gradlew test` succeed without manual intervention, given Docker is running.
- `./gradlew bootRun` starts the app against a real MySQL instance via Docker Compose, with Flyway and JPA both connecting successfully.
- `GET /actuator/health` returns `200` with `status: UP`.
- Docker-related files live under `docker/`; Spring config files follow `application-<profile>.yml` naming.
- No business/domain code was introduced.

## Definition of Done

Met per `docs/DEVELOPMENT.md` §11, except the two steps that require an independent Review Agent and human approval — those are still outstanding (see below).

## Open Questions

None blocking. Deferred, not decided here (do not resolve by assumption):

- Whether/when to add `application-prod.yml` — deferred until a real deployment target exists.
- Whether to pin an explicit `spring.jpa.hibernate.ddl-auto: validate` once the first Flyway migration exists — deferred to the task that adds the first domain table.
- CI (GitHub Actions or similar) — not set up; the repository is not git-initialized yet, so this is out of scope for now.

## Related ADRs

None. This task did not require a new architectural decision — it applied existing decisions in `docs/ARCHITECTURE.md` (MySQL, Flyway, single monolith) to the runtime/dev environment.
