# ───────── Build stage ──────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon \
    && mv build/libs/*.jar app.jar

# ───────── Runtime stage ─────────
FROM eclipse-temurin:21-jre-jammy AS run
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app

RUN groupadd --system appuser && useradd --system --gid appuser appuser
COPY --from=build --chown=appuser:appuser /workspace/app.jar .

USER appuser

EXPOSE 4001
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD \
  curl -f http://localhost:4001/actuator/health || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
