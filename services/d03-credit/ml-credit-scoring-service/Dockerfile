# MS-06 · ml-credit-scoring-service · Java 21 / Spring Boot
# Build context: repository root
#   docker build -f services/d03-credit/ml-credit-scoring-service/Dockerfile -t solidariaplus/ml-credit-scoring-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d03-credit/ml-credit-scoring-service services/d03-credit/ml-credit-scoring-service
RUN chmod +x gradlew && ./gradlew :ml-credit-scoring-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d03-credit/ml-credit-scoring-service/build/libs/app.jar app.jar
USER app
EXPOSE 8086
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
