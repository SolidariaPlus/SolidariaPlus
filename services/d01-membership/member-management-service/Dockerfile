# MS-01 · member-management-service · Java 21 / Spring Boot
# Build context: repository root
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
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app app
COPY --from=builder /workspace/services/d01-membership/member-management-service/build/libs/app.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
