# Vowser Backend

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-Apache_2.0-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.7-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blueviolet)

The Central Communication & Control Hub for the Vowser Ecosystem.

This repository contains the source code for the backend server of Vowser, which acts as an API gateway and a real-time control hub. It connects the `vowser-client` (the user's interface) to the `vowser-mcp-server` (the AI analysis engine).

## Architecture

Vowser operates on a distributed architecture to maximize performance and maintainability. This `vowser-backend` server plays the role of the **"Spine" (척추)**, connecting the "Brain" and the "Face/Hands".

```
[vowser-client] <=> [vowser-backend (Kotlin)] <=> [vowser-mcp-server (Python)]
```

- **`vowser-client`**: The user-facing application (e.g., browser extension) that handles UI and executes final browser commands.
- **`vowser-backend` (This Repository)**: Orchestrates the flow. It receives requests from the client, forwards analysis tasks to the Python server, and relays control commands to the client via WebSockets.
- **`vowser-mcp-server`**: The AI engine that performs heavy tasks like webpage analysis, knowledge graph creation, and natural language understanding.

## Code Structure

The project follows a layered architecture to separate concerns.

```
src
└── main
    └── kotlin
        └── com
            └── vowser
                └── backend
                    ├── Application.kt
                    ├── api             # 1. API Layer: Handles external requests
                    │   ├── dto         #    - Data Transfer Objects (Request/Response)
                    │   └── controller  #    - REST API Controllers
                    ├── application     # 2. Application Layer: Business logic
                    │   └── service     #    - Core service logic (caching, orchestration)
                    └── infrastructure  # 3. Infrastructure Layer: External system integration
                        ├── client      #    - FeignClient interface (for Python server)
                        ├── config      #    - Configuration classes (Feign, WebSocket, etc.)
                        └── control     #    - WebSocket handlers, BrowserTool interface & implementations
```

## Features

- **API Gateway**: Provides stable REST APIs for the client to request analysis and query information.
- **Real-time Control Hub**: Manages WebSocket connections with the client to relay browser control commands (e.g., click, type, go back).
- **Performance Optimization**: Implements caching strategies to reduce redundant, costly AI analysis calls.
- **Stateless & Scalable**: Designed to be stateless for easy horizontal scaling.

## Getting Started

### Prerequisites

- JDK 21
- Gradle 8.x

### Configuration

Before running the application, you need to configure the address of the Python AI engine (`vowser-mcp-server`).

Create or modify `src/main/resources/application-local.yml` and add the following:

```yaml
mcp:
  python:
    server:
      url: http://localhost:8000 # URL of the running vowser-mcp-server
```

### Running the Application

```bash
./gradlew bootRun
```

The server will start on port `8080` by default.

## API Endpoints

### REST API

- `POST /api/v1/analyze`: Requests analysis of a new webpage.
- `POST /api/v1/query`: Asks a natural language question about a webpage.
- `GET /api/v1/graph/visualize`: Retrieves graph data in a D3.js-friendly format for visualization.

### WebSocket API

- `ws://localhost:8080/control`: The endpoint for establishing a real-time control connection with the `vowser-client`. It accepts `CallToolRequest` messages to execute browser actions.

## License

This project is licensed under the Apache 2.0 License.

---

# Vowser Backend

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-Apache_2.0-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.7-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blueviolet)

Vowser 생태계를 위한 중앙 통신 및 제어 허브입니다.

이 레포지토리는 Vowser 백엔드 서버의 소스 코드를 포함하며, API 게이트웨이 및 실시간 제어 허브 역할을 수행합니다. `vowser-client`(사용자 인터페이스)와 `vowser-mcp-server`(AI 분석 엔진) 사이를 연결합니다.

## 아키텍처

Vowser는 성능과 유지보수성을 극대화하기 위해 분산 아키텍처로 동작합니다. 이 `vowser-backend` 서버는 **'척추(Spine)'** 역할을 맡아, '두뇌'와 '얼굴과 손발'을 연결합니다.

```
[vowser-client] <=> [vowser-backend (Kotlin)] <=> [vowser-mcp-server(Python)]
```

- **`vowser-client`**: UI를 처리하고 최종적인 브라우저 명령을 실행하는 사용자 대면 애플리케이션(예: 브라우저 확장 프로그램)입니다.
- **`vowser-backend` (현재 레포지토리)**: 전체 흐름을 조율합니다. 클라이언트의 요청을 받아 분석 작업을 Python 서버에 전달하고, 제어 명령을 웹소켓을 통해 클라이언트에 중계합니다.
- **`vowser-mcp-server`**: 웹페이지 분석, 지식 그래프 생성, 자연어 이해 등 무거운 작업을 수행하는 AI 엔진입니다.

## 코드 구조

이 프로젝트는 역할과 책임 분리를 위한 계층형 아키텍처를 따릅니다.

```
src
└── main
    └── kotlin
        └── com
            └── vowser
                └── backend
                    ├── Application.kt
                    ├── api             # 1. API 계층: 외부 요청 처리
                    │   ├── dto         #    - 데이터 전송 객체 (Request/Response)
                    │   └── controller  #    - REST API 컨트롤러
                    ├── application     # 2. 애플리케이션 계층: 비즈니스 로직
                    │   └── service     #    - 핵심 서비스 로직 (캐싱, 오케스트레이션)
                    └── infrastructure  # 3. 인프라 계층: 외부 시스템 연동
                        ├── client      #    - FeignClient 인터페이스 (Python 서버 연동)
                        ├── config      #    - Feign, WebSocket 등 설정 클래스
                        └── control     #    - 웹소켓 핸들러, BrowserTool 인터페이스 및 구현체
```

## 주요 기능

- **API 게이트웨이**: 클라이언트가 분석 및 정보 조회를 요청할 수 있는 안정적인 REST API를 제공합니다.
- **실시간 제어 허브**: 클라이언트와의 웹소켓 연결을 관리하여 브라우저 제어 명령(클릭, 타이핑, 뒤로가기 등)을 중계합니다.
- **성능 최적화**: 캐싱 전략을 구현하여 비용이 많이 드는 중복 AI 분석 호출을 줄입니다.
- **무상태 및 확장성**: 수평적 확장이 용이하도록 무상태(Stateless)로 설계되었습니다.

---

## 시작하기

### 사전 요구사항

- JDK 21
- Gradle 8.x

### 설정

애플리케이션을 실행하기 전에, Python AI 엔진(`vowser-mcp-server`)의 주소를 설정해야 합니다.

`src/main/resources/application-local.yml` 파일을 생성하거나 수정하여 아래 내용을 추가하세요.

```yaml
mcp:
  python:
    server:
      url: http://localhost:8000 # 실행 중인 vowser-mcp-server의 주소
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

서버는 기본적으로 `8080` 포트에서 시작됩니다.

## API 엔드포인트

### REST API

- `POST /api/v1/analyze`: 새로운 웹페이지의 분석을 요청합니다.
- `POST /api/v1/query`: 웹페이지에 대해 자연어 질문을 합니다.
- `GET /api/v1/graph/visualize`: D3.js 시각화를 위한 그래프 데이터를 조회합니다.

### WebSocket API

- `ws://localhost:8080/control`: `vowser-client`와 실시간 제어 연결을 수립하기 위한 엔드포인트입니다. 브라우저 액션을 실행하기 위한 `CallToolRequest` 메시지를 받습니다.

## 라이선스

이 프로젝트는 Apache 2.0 라이선스를 따릅니다.