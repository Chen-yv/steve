# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SteVe (SteckdosenVerwaltung) is an open-source OCPP (Open Charge Point Protocol) server for managing electric vehicle charging stations. It supports OCPP versions 1.2, 1.5, and 1.6 over both SOAP and WebSocket/JSON transports.

## Tech Stack

- Java 21+ (Spring Boot 4.x)
- Maven build system
- MySQL or MariaDB database
- jOOQ for database access (not JPA/Hibernate)
- Apache CXF for SOAP services
- WebSocket for OCPP JSON transport

## Build Commands

```bash
# Build for production (MySQL, skips tests by default)
./mvnw package -Pprod,mysql

# Build for production (MariaDB)
./mvnw package -Pprod,mariadb

# Build and run tests (requires running database)
./mvnw -Ptest,mysql clean package

# Run the application
java -jar target/steve.war

# Run with specific profile
java -jar target/steve.war --spring.profiles.active=dev
```

## Architecture

### OCPP Protocol Layer (`de.rwth.idsg.steve.ocpp`)

- **Transport abstraction**: `OcppTransport` enum (SOAP, JSON) with version-specific implementations
- **WebSocket pipeline**: `ocpp.ws.pipeline` handles incoming/outgoing message processing
- **Version handlers**: Separate endpoint classes in `ocpp.ws.ocpp12`, `ocpp.ws.ocpp15`, `ocpp.ws.ocpp16`
- **SOAP services**: `ocpp.soap` contains CXF service implementations
- **Communication tasks**: `CommunicationTask` hierarchy manages async charge point operations

### Repository Layer (`de.rwth.idsg.steve.repository`)

- Uses jOOQ for type-safe SQL queries (not JPA)
- Interface + `impl` pattern: e.g., `ChargePointRepository` / `ChargePointRepositoryImpl`
- `RepositoryUtils` provides common query helpers

### Web Layer

- **Manager UI**: `web.controller` - Thymeleaf-based admin interface at `/manager`
- **REST APIs**: `web.api` - JSON APIs at `/api` (requires API key auth)
- **DTOs**: `web.dto` contains request/response objects with validation

### Configuration

- `application-{profile}.properties`: Environment-specific config (prod, dev, docker, test)
- `application.yml`: Main Spring Boot config with property binding
- `SteveProperties.java`: Type-safe configuration binding
- Database config, auth credentials, OCPP protocol settings in properties files

## Key Patterns

- **License header**: All Java files must include GPL license header (see `LICENSE-HEADER.txt`)
- **Checkstyle**: Code style enforced via `checkstyle.xml` (120 char line limit, no tabs, no trailing spaces)
- **jOOQ code generation**: Database schema changes require regenerating jOOQ classes
- **OCPP versioning**: New OCPP features need implementations in both SOAP and WebSocket paths

## Testing

Integration tests (`*IT` suffix) require a running MySQL/MariaDB database. The CI workflow shows the test database setup process. Unit tests can run without database.

## Docker Development

```bash
docker compose up -d
```

Uses `application-docker.properties` for configuration. Web UI at `http://localhost:8180/steve/manager`.
