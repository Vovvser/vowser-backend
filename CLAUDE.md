# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vowser Backend is a Spring Boot 3.2.7 application (Java 21) that serves as the central communication hub between vowser-client (browser extension) and vowser-mcp-server (Python AI analysis engine). It provides REST APIs, WebSocket communication for real-time browser control, and integrates with speech-to-text services.

## Commands

### Development
```bash
# Run application locally
./gradlew bootRun

# Build JAR
./gradlew bootJar

# Run tests
./gradlew test

# Clean and build
./gradlew clean build

# Run with specific profile
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### Docker
```bash
# Start all services (MariaDB, Redis, Backend)
docker-compose up -d

# Rebuild backend container only
docker-compose up -d --build backend

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

### Testing Single Files
```bash
# Run specific test class
./gradlew test --tests SpeechControllerUnitTest

# Run with debug output
./gradlew test --info
```

## Architecture

The codebase follows a **layered architecture**:

1. **api/** - REST controllers, DTOs, and API documentation
   - Controllers handle HTTP/WebSocket requests
   - DTOs separate API contracts from domain models

2. **application/** - Business logic and service layer
   - Core services orchestrate business operations
   - Speech processing and MCP integration logic

3. **domain/** - Business entities and repositories
   - JPA entities and repository interfaces

4. **infrastructure/** - External integrations and configuration
   - Security (JWT, OAuth2)
   - WebSocket handlers for browser control
   - Tool implementations for browser actions
   - Configuration classes

### Key Components

**WebSocket Control Flow**:
- `ControlWebSocketHandler` receives commands from vowser-client at `ws://localhost:4001/control`
- `ToolRegistry` maps tool names to implementations
- Tools execute browser actions (click, type, navigate, etc.)
- Responses relay back through WebSocket

**Speech Processing Pipeline**:
1. `SpeechController` receives audio/text
2. `SpeechProcessingService` determines mode (command/contribution)
3. `McpIntegrationService` communicates with MCP server for AI analysis
4. Results sent back via REST response

**Authentication**:
- OAuth2 with Naver for social login
- JWT tokens for stateless authentication
- `SecurityConfig` defines public/protected endpoints

## Configuration

### Environment Setup
1. Copy `.env.example` to `.env`
2. Configure required variables:
   - Database: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
   - Redis: `REDIS_HOST`, `REDIS_PORT`
   - OAuth2: `OAUTH2_NAVER_CLIENT_ID`, `OAUTH2_NAVER_CLIENT_SECRET`
   - JWT: `JWT_SECRET` (min 32 chars)

### Application Profiles
- **local**: Development with detailed logging
- **deploy**: Production with file logging to `/var/log/vowser/`
- **test**: Testing with in-memory database

### Key Endpoints
- REST API: `http://localhost:8080/api/v1/*`
- WebSocket: `ws://localhost:8080/control`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Important Patterns

### Adding New Browser Tools
1. Create class implementing `Tool` interface in `infrastructure/control/tool/`
2. Register in `ToolRegistry` constructor
3. Tool receives `CallToolRequest` and returns response object

### Adding New API Endpoints
1. Create controller in `api/controller/`
2. Define request/response DTOs in `api/dto/`
3. Implement service logic in `application/service/`
4. Add Swagger annotations for documentation

### WebSocket Message Format
```json
{
  "tool": "click",
  "arguments": {
    "selector": "#button-id"
  }
}
```

### MCP Server Integration
- Base URL configured in `application-*.yml` as `mcp.server.base-url`
- `McpIntegrationService` handles all MCP communication
- Endpoints: `/process-command`, `/process-contribution`

## Dependencies & Versions

- Java 21 (Eclipse Temurin)
- Spring Boot 3.2.7
- MariaDB 10.11
- Redis 7.2
- Gradle 8.x
- Key libraries:
  - Spring WebSocket for real-time communication
  - Spring Data JPA with Hibernate
  - Spring Security with JWT (jjwt)
  - OpenFeign for HTTP clients
  - Google Cloud Speech 4.30.0
  - Swagger/OpenAPI (springdoc) 2.5.0