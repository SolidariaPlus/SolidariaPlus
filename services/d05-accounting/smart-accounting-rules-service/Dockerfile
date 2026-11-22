# MS-12 · smart-accounting-rules-service · Java 21 / Spring Boot
# Build context: raíz del repositorio
#   docker build -f services/d05-accounting/smart-accounting-rules-service/Dockerfile -t solidariaplus/smart-accounting-rules-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d05-accounting/smart-accounting-rules-service services/d05-accounting/smart-accounting-rules-service
RUN chmod +x gradlew && ./gradlew :smart-accounting-rules-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d05-accounting/smart-accounting-rules-service/build/libs/app.jar app.jar
USER app
EXPOSE 8092
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
