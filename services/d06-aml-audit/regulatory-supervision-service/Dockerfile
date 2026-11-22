# MS-16 · regulatory-supervision-service · Java 21 / Spring Boot
# Build context: raíz del repositorio
#   docker build -f services/d06-aml-audit/regulatory-supervision-service/Dockerfile -t solidariaplus/regulatory-supervision-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d06-aml-audit/regulatory-supervision-service services/d06-aml-audit/regulatory-supervision-service
RUN chmod +x gradlew && ./gradlew :regulatory-supervision-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d06-aml-audit/regulatory-supervision-service/build/libs/app.jar app.jar
USER app
EXPOSE 8096
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
