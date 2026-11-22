# MS-02 · smart-kyc-onboarding-service · Java 21 / Spring Boot
# Build context: repository root
#   docker build -f services/d01-membership/smart-kyc-onboarding-service/Dockerfile -t solidariaplus/smart-kyc-onboarding-service .

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d01-membership/smart-kyc-onboarding-service services/d01-membership/smart-kyc-onboarding-service
RUN chmod +x gradlew && ./gradlew :smart-kyc-onboarding-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=builder /workspace/services/d01-membership/smart-kyc-onboarding-service/build/libs/app.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
