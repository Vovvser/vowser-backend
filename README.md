# Vowser-Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.7-brightgreen.svg?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?logo=mysql&logoColor=white)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The Orchestration Layer for the Vowser Ecosystem.

This repository hosts the backend services that power Vowser. It authenticates users, processes voice commands, connects to the MCP agent, and dispatches real-time browser automation commands to the client.

## Architecture

Vowser operates on a distributed architecture. The `vowser-backend` acts as the **Spine** that connects the user-facing client and the AI agent.

`[vowser-client (Kotlin)] <=> [vowser-backend (Spring Boot)] <=> [vowser-agent-server (Python)]`

- **vowser-client:** Compose Multiplatform desktop app that records voice and renders browser automation feedback.
- **vowser-backend (This Repository):** Routes authentication, speech processing, accessibility personalization, and WebSocket control streams.
- **vowser-agent-server:** MCP-based AI worker that interprets transcripts and generates browser actions.

## Code Structure

```
.
├── src
│   ├── main
│   │   ├── java/com/vowser/backend
│   │   │   ├── VowserBackendApplication.java      # Spring Boot entry point
│   │   │   ├── api/                               # REST controllers, DTOs, Swagger docs
│   │   │   ├── application/                       # Orchestration services (auth, speech, control)
│   │   │   ├── domain/                            # JPA entities and repositories
│   │   │   ├── infrastructure/                    # Config, WebSocket, security, MCP client, tools
│   │   │   └── common/                            # Constants, enums, exceptions, shared utilities
│   │   └── resources/                             # Profile-specific YAML and Thymeleaf templates
│   └── test                                       # JUnit tests with audio fixtures & configs
├── docker-compose.yml                             # Local stack with MySQL, Redis, backend
├── Dockerfile                                     # Production-ready image build
└── infra/                                         # Jenkins pipeline and deployment assets
```

## Features

- **Voice Pipeline Orchestrator:** Normalizes number/alphabet modes and proxies speech recognition to Naver Cloud STT and Google Cloud Speech.
- **Authentication & Session Management:** Naver OAuth2 login, JWT issuance, and refresh token storage backed by Redis.
- **Real-time Control Bridge:** WebSocket broker that relays MCP agent responses to connected clients with pluggable browser tools.
- **Accessibility Personalization:** Manages encrypted accessibility profiles and exposes tailored settings to the client.
- **Path Insights & Analytics:** Provides MCP-facing APIs for path submission, popularity tracking, and graph statistics.
- **Operations Ready:** Ships with Actuator probes, Swagger UI, caching controls, and structured logging for production monitoring.

---

## Getting Started

### Prerequisites

- **JDK 21**
- **Gradle Wrapper** (included)
- **MySQL 8.x** and **Redis 7.x** (local installations or Docker Compose)
- Credentials for **Naver Cloud STT** and **Google Cloud Speech**
- OAuth client credentials for **Naver Login**

### Environment Setup

1. Create a `.env.local` file (used by `docker-compose.yml`) with the core variables:

   ```dotenv
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=vowser
   DB_USERNAME=your_db_user
   DB_PASSWORD=your_db_password
   DB_ROOT_PASSWORD=your_root_password
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=your_redis_password
   JWT_SECRET=change_this_secret
   JWT_ACCESS_TOKEN_VALIDITY=900
   JWT_REFRESH_TOKEN_VALIDITY=1209600
   JWT_ISSUER=vowser-backend
   OAUTH2_NAVER_CLIENT_ID=your_oauth_client_id
   OAUTH2_NAVER_CLIENT_SECRET=your_oauth_client_secret
   OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/naver
   OAUTH2_SUCCESS_REDIRECT_URL=http://localhost:5173/auth/callback
   NAVER_CLOUD_CLIENT_ID=your_naver_cloud_id
   NAVER_CLOUD_CLIENT_SECRET=your_naver_cloud_secret
   NAVER_CLOUD_STT_URL=https://naveropenapi.apigw.ntruss.com/recog/v1/stt
   GOOGLE_CREDENTIALS_JSON={"type":"service_account",...}
   VOWSER_DB_ENCRYPTION_KEY=32_byte_hex_or_base64_key
   ```

2. Review `src/main/resources/application-local.yml` to adjust database or logging preferences.
3. Ensure the MCP agent URL (`mcp.python.server.url`) points to the running `vowser-agent-server`.

### Run Locally

```bash
./gradlew bootRun
```

The server listens on `http://localhost:8080`. Swagger UI is available at `/swagger-ui.html`, and health checks at `/actuator/health`.

### Run with Docker Compose

```bash
docker compose up --build
```

This spins up MySQL, Redis, and the backend container. The backend is exposed on port `4001` inside the compose network.

### Companion Services

- [vowser-client](https://github.com/Vovvser/vowser-client)
- [vowser-agent-server](https://github.com/Vovvser/vowser-agent-server)

## Roadmap

- Harden WebSocket reconnection and monitoring for MCP sessions.
- Add adaptive failover across speech providers.
- Ship admin-facing dashboards for path analytics and tooling telemetry.
- Expand integration tests to cover OAuth2 and speech-processing edge cases.

For contribution guidelines and community standards, see [`CONTRIBUTING.md`](./CONTRIBUTING.md) and [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md).

## License

This project is licensed under the Apache 2.0 License.

---

## Testing & Quality Checks

- **All tests:** `./gradlew test`
- **Speech pipeline focus:** `./gradlew test --tests "*speech.*"`
- **Static checks:** `./gradlew check`

## Support & Contact

- **Security disclosures:** vowser_security@gmail.com

## Releases

- 0.0.1 — Initial backend alpha delivering speech processing, OAuth, and WebSocket orchestration.

# Vowser-Backend (Korean)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.7-brightgreen.svg?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?logo=mysql&logoColor=white)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Vowser의 인증, 음성 처리, 실시간 브라우저 제어를 담당하는 Vowser 백엔드 서비스를 포함합니다.

## 아키텍처

Vowser는 분산 아키텍처로 동작합니다. `vowser-backend`는 클라이언트와 AI 에이전트를 연결하는 역할을 합니다.

`[vowser-client (Kotlin)] <=> [vowser-backend (Java)] <=> [vowser-agent-server (Python)]`

## 코드 구조

```
.
├── src
│   ├── main
│   │   ├── java/com/vowser/backend
│   │   │   ├── VowserBackendApplication.java 
│   │   │   ├── api/                               # REST 컨트롤러, DTO, Swagger 문서화
│   │   │   ├── application/                       # 인증·음성·제어 등 서비스 로직
│   │   │   ├── domain/                            # JPA 엔티티와 리포지토리
│   │   │   ├── infrastructure/                    # 설정, WebSocket, 보안, MCP 클라이언트, 툴
│   │   │   └── common/                            # 상수, 열거형, 예외, 공통 유틸리티
│   │   └── resources/                             # 프로필별 YAML, Thymeleaf 템플릿
│   └── test                                       # 오디오 픽스처를 포함한 JUnit 테스트
├── docker-compose.yml                             # MySQL·Redis·백엔드 로컬 스택
├── Dockerfile                                     # 운영 배포용 이미지
└── infra/                                         # Jenkins 파이프라인 및 배포 관련 파일
```

## 주요 기능

- **음성 처리 파이프라인:** 숫자·알파벳 모드 정규화 후 Naver Cloud STT를 통해 음성 인식.
- **인증 및 세션 관리:** Naver OAuth2 로그인, JWT 발급, Redis 기반 리프레시 토큰 저장.
- **실시간 제어 브리지:** Vower-Agent-Server 응답을 WebSocket으로 클라이언트에 중계.
- **접근성 개인화:** 암호화된 접근성 프로필을 관리하고 클라이언트에 맞춤 설정 제공.
- **경로 인사이트 & 분석:** 경로 저장, 인기 경로 조회, 그래프 통계를 제공하는 Vower-Agent-Server 연동 API.
- **운영 친화성:** Actuator, Swagger UI, 캐싱 제어, 구조화 로그 등 운영 모니터링 기능을 내장.

---

## 시작하기

### 사전 요구사항

- **JDK 21**
- **Gradle Wrapper** (레포지토리 포함)
- **MySQL 8.0.43**, **Redis 7.0.15** (로컬 설치 또는 Docker Compose)
- **Naver Cloud STT** 자격 증명
- **Naver 로그인** OAuth 클라이언트 자격 증명

### 환경 설정

1. `docker-compose.yml`과 함께 사용할 `.env.local` 파일을 생성하고 필수 환경변수를 정의합니다.

   ```dotenv
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=vowser
   DB_USERNAME=데이터베이스_사용자
   DB_PASSWORD=데이터베이스_비밀번호
   DB_ROOT_PASSWORD=루트_비밀번호
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=redis_비밀번호
   JWT_SECRET=변경_필수_시크릿
   JWT_ACCESS_TOKEN_VALIDITY=900
   JWT_REFRESH_TOKEN_VALIDITY=1209600
   JWT_ISSUER=vowser-backend
   OAUTH2_NAVER_CLIENT_ID=OAuth_클라이언트_ID
   OAUTH2_NAVER_CLIENT_SECRET=OAuth_클라이언트_시크릿
   OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/naver
   OAUTH2_SUCCESS_REDIRECT_URL=http://localhost:5173/auth/callback
   NAVER_CLOUD_CLIENT_ID=Naver_Cloud_ID
   NAVER_CLOUD_CLIENT_SECRET=Naver_Cloud_Secret
   NAVER_CLOUD_STT_URL=https://naveropenapi.apigw.ntruss.com/recog/v1/stt
   GOOGLE_CREDENTIALS_JSON={"type":"service_account",...}
   VOWSER_DB_ENCRYPTION_KEY=32바이트_키
   ```

2. `src/main/resources/application-local.yml`을 검토하여 DB, 로깅, 캐싱 설정을 필요에 맞게 조정합니다.
3. `mcp.python.server.url`이 실행 중인 `vowser-agent-server`를 가리키도록 확인합니다.

### 로컬 실행

```bash
./gradlew bootRun
```

기본 포트는 `http://localhost:8080`이며, Swagger UI는 `/swagger-ui.html`, 헬스 체크는 `/actuator/health`에서 확인할 수 있습니다.

### Docker Compose 실행

```bash
docker compose up --build
```

MySQL, Redis, 백엔드 컨테이너가 함께 실행되며, 백엔드는 Compose 네트워크에서 `4001` 포트로 노출됩니다.

### 연동 서비스

- [vowser-client](https://github.com/Vovvser/vowser-client)
- [vowser-agent-server](https://github.com/Vovvser/vowser-agent-server)

## 로드맵

- MCP 세션 재연결 및 모니터링 고도화
- 음성 인식 프로바이더 자동 폴백 지원
- 경로 분석 및 도구 텔레메트리용 관리자 대시보드 공개
- OAuth2 및 음성 파이프라인 통합 테스트 확대

기여 가이드와 커뮤니티 행동 강령은 [`CONTRIBUTING.md`](./CONTRIBUTING.md), [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md)를 참고하세요.

## 라이선스

이 프로젝트는 Apache 2.0 라이선스를 따릅니다.

---

## 테스트 및 품질 확인

- **전체 테스트:** `./gradlew test`
- **음성 파이프라인 집중:** `./gradlew test --tests "*speech.*"`
- **정적 검사:** `./gradlew check`

## 지원 및 문의

- **보안 제보:** vowser_security@gmail.com

## 릴리스

- 0.0.1 — 음성 처리, OAuth, WebSocket 오케스트레이션을 포함한 백엔드 알파 버전
