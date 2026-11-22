# MS-17 · identity-access-service · Java 21 / Spring Boot
# Build context: repository root
#   docker build -f services/d07-identity/identity-access-service/Dockerfile -t solidariaplus/identity-access-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d07-identity/identity-access-service services/d07-identity/identity-access-service
RUN chmod +x gradlew && ./gradlew :identity-access-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d07-identity/identity-access-service/build/libs/app.jar app.jar
USER app
EXPOSE 8097
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
