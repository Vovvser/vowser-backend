# ───────── Build stage ──────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :infrastructure:bootJar \
    && mv infrastructure/build/libs/*.jar app.jar

# ───────── Runtime stage ─────────
FROM eclipse-temurin:21-jre-jammy AS run
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app
COPY --from=build /workspace/app.jar .
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD \
  curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
