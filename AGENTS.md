# Repository Guidelines

SteVe (SteckdosenVerwaltung) is an open-source OCPP server for managing EV charging stations, supporting OCPP 1.2/1.5/1.6 over SOAP and WebSocket/JSON.

## Project Structure & Module Organization

- `src/main/java/de/rwth/idsg/steve/` — Java 25 / Spring Boot source, organized by layer:
  - `ocpp/` — protocol layer (`ws/` WebSocket pipeline, `soap/` CXF services, versioned handlers in `ocpp12/15/16`)
  - `repository/` — jOOQ database access, Interface + `impl` pattern (e.g. `ChargePointRepository` / `ChargePointRepositoryImpl`)
  - `web/` — `controller/` Thymeleaf admin UI (`/manager`), `api/` REST APIs (`/api`), `dto/` validated request/response objects
- `src/main/resources/db/migration/` — Flyway SQL migrations (never edit applied migrations; add new ones)
- `src/main/webapp/` — JSP views and static assets (css/js/images)
- `src/test/java/` — tests mirroring the main package layout; `src/test/resources/certificates/` for test fixtures
- `website/`, `k8s/`, `docker-compose.yml` — docs site and deployment configs

## Build, Test, and Development Commands

```bash
./mvnw package -Pprod,mysql        # production build (skips tests)
./mvnw package -Pprod,mariadb      # production build for MariaDB
./mvnw -Ptest,mysql clean package  # build and run tests (needs a running DB)
./mvnw test -Dtest=ClassName       # run a single test
java -jar target/steve.war         # run the app (--spring.profiles.active=dev for dev)
```

## Coding Style & Naming Conventions

- 4-space indentation; follow the existing file layout and license header (`LICENSE-HEADER.txt`).
- Checkstyle runs via Maven using `checkstyle.xml`; keep new code compliant.
- Use jOOQ for all database access — do not introduce JPA/Hibernate entities.
- Repositories follow the Interface + `impl` pattern; DTOs use Bean Validation annotations.

## Testing Guidelines

- JUnit-based tests; name classes `*Test` (e.g. `ApplicationJsonTest.java`).
- Tests require a running MySQL/MariaDB instance via the `test` profile.
- Add tests for new OCPP handlers and repository logic; a feature is not done until its tests pass.

## Commit & Pull Request Guidelines

- Write short, imperative commit messages describing the change (see history, e.g. "fix builds", "introduce GroupedOpenApi for API docs").
- Commit only complete, tested functionality (e.g. submit card authentication only after the feature works and tests pass) — no partial work-in-progress commits.
- Never commit bare submodule-pointer changes.
- PRs should describe the motivation and approach, link related issues, and keep one logical change per PR with CI green before merge.

## Repository Remotes, Branches & Sync

Three remotes are configured. Local `master` is our working branch and must stay in sync with BOTH our GitHub repo and the internal GitLab branch after every change set:

| Remote | URL | Role |
|---|---|---|
| `origin` | `git@github.com:steve-community/steve.git` | Upstream official repo; fetch only, never push |
| `github` | `git@github.com:Chen-yv/steve.git` | Our repo; `git push github` pushes local `master` → `master` |
| `gitlab` | `ssh://git@192.168.1.205:9022/30179/ocpp-steve.git` | Internal GitLab; `git push gitlab` pushes local `master` → `injet/master` (refspec preconfigured) |

Routine sync after committing on local master: `git push github && git push gitlab`.

Internal GitLab branch roles (`30179/ocpp-steve`):

- `injet/master` — mirror of our local master; our own development and fixes land here.
- `injet/steve-3.14.1-patched` — the PRODUCTION deployment branch (log viewer customizations, sign-in audit, Jetty 12.1.14 bump, production config). Fixes intended for production must target THIS branch via MR, not `injet/master`.
- `master` — plain upstream snapshot.

Creating an MR on the internal GitLab works over SSH push options (no API token configured on this machine):

```bash
git push gitlab <task-branch> \
  -o merge_request.create \
  -o merge_request.target=injet/steve-3.14.1-patched \
  -o merge_request.title="..." -o merge_request.description="..."
```

Reviewer assignment is NOT possible via push options; assign in the web UI (or an API token is needed). Do not commit credentials (DB passwords, web passwords, tokens) into this repo — `github` is a public-facing host.

## Local Environment & Production Facts

- Build with JDK 25 (`pom.xml java.version=25`, class file 69): `export JAVA_HOME=/home/zcl/jdks/jdk-25.0.4.1+1`. The system default `/usr/bin/java` is JDK 21 and CANNOT build this project.
- Local MySQL runs on `localhost:3306` (schema `stevedb`, dev password `changeme`). Unit tests without a fresh DB migration:
  ```bash
  ./mvnw -Pprod,mysql test -Dtest=CentralSystemService16ServiceTest \
    -DskipTests=false -Dflyway.skip=true
  ```
  On branches carrying the production config (db port 3307 / production credentials in `application-prod.properties`), override for local runs: `-Ddb.ip=localhost -Ddb.port=3306 -Ddb.password=changeme`.
- The local systemd `steve.service` (port 1234, drop-in `jdk25.conf`) is a LOCAL instance only — it is NOT production.
- Production is the remote server behind `http://ij.wydq.tech/steve/` (SteVe 3.14.1, log file `/home/www/logs/steve.log`, manager account `wyadmin`). Because 3.14.1 is compiled with Java 25, the production JVM must be >= 25; exact minor version unverified (no shell access from here).
- Production deployment = build war from `injet/steve-3.14.1-patched`, replace war on the production server, restart the service there. Requires explicit user confirmation.

## Known Issue: Zombie Active Transactions (fix in MR !1)

Root cause chain, fully evidenced on 2026-11-09 (charge point `ampax1.0_EU_chengdu`, transactions 11342/11344/11345 stuck "active" on the platform):

1. `AuthTagServiceLocal.decideStatus` rejects StartTransaction with `ConcurrentTx` when the idTag's ACTIVE transaction count >= its `maxActiveTransactionCount` (default 1). The count is PER-IDTAG, platform-wide, not per connector.
2. `CentralSystemService16_Service.startTransaction()` used to `insertTransaction()` UNCONDITIONALLY, even for rejected starts — each rejection created another permanently-active row, amplifying the lockout (the station never sends StopTransaction for a rejected start, and RemoteStopTransaction for such ids is rejected by the station because no local transaction exists).
3. Original poison row: tx 10051 on `test_wql` (idTag `080101080019`, started 2026-09-01, never stopped).

Fix (commit d3e82fc7 on branch `fix/starttransaction-rejected-zombie`, MR !1 → `injet/steve-3.14.1-patched`, reviewer 姚力文): non-ACCEPTED starts return `transactionId=0` without inserting a row or publishing `OcppTransactionStarted`; `CentralSystemService16ServiceTest` (5 cases) passes on the production baseline; `Issue73FixTest` updated accordingly.

Still open after the MR merges: rebuild + redeploy production, and clean up the EXISTING zombie rows on production (10051, 11342, 11344, 11345) via the manager UI transaction stop actions — the fix only prevents NEW zombies. The pile-side OCPP 1.6J plugin intentionally never sends StopTransaction for rejected starts (see ocpp repo `TransactionManager.cpp` ~line 1896); a station-side stop-on-refusal is a separate, optional hardening topic.

