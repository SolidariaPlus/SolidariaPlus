# MS-03 · core-savings-service · Java 21 / Spring Boot
# Build context: raíz del repositorio
#   docker build -f services/d02-savings/core-savings-service/Dockerfile -t solidariaplus/core-savings-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d02-savings/core-savings-service services/d02-savings/core-savings-service
RUN chmod +x gradlew && ./gradlew :core-savings-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d02-savings/core-savings-service/build/libs/app.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
