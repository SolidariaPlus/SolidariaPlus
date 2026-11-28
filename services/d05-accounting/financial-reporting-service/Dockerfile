# MS-13 · financial-reporting-service · Java 21 / Spring Boot
# Build context: repository root
#   docker build -f services/d05-accounting/financial-reporting-service/Dockerfile -t solidariaplus/financial-reporting-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d05-accounting/financial-reporting-service services/d05-accounting/financial-reporting-service
RUN chmod +x gradlew && ./gradlew :financial-reporting-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app app
COPY --from=builder /workspace/services/d05-accounting/financial-reporting-service/build/libs/app.jar app.jar
USER 10001:10001
EXPOSE 8093
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
