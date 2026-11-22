# MS-09 · vault-reconciliation-service · Java 21 / Spring Boot
# Build context: repository root
#   docker build -f services/d04-teller-treasury/vault-reconciliation-service/Dockerfile -t solidariaplus/vault-reconciliation-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d04-teller-treasury/vault-reconciliation-service services/d04-teller-treasury/vault-reconciliation-service
RUN chmod +x gradlew && ./gradlew :vault-reconciliation-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d04-teller-treasury/vault-reconciliation-service/build/libs/app.jar app.jar
USER app
EXPOSE 8089
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
