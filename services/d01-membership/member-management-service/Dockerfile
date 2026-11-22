# MS-01 · member-management-service · Java 21 / Spring Boot
# Build context: raíz del repositorio
#   docker build -f services/d01-membership/member-management-service/Dockerfile -t solidariaplus/member-management-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d01-membership/member-management-service services/d01-membership/member-management-service
RUN chmod +x gradlew && ./gradlew :member-management-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d01-membership/member-management-service/build/libs/app.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
