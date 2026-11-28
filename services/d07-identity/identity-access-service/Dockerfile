FROM node:24-alpine AS portal
WORKDIR /workspace
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc turbo.json ./
COPY packages packages
COPY apps/auth-portal apps/auth-portal
RUN pnpm install --frozen-lockfile --ignore-scripts --filter "@solidariaplus/auth-portal..." \
    && pnpm turbo run build --filter "@solidariaplus/auth-portal..."

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services/d07-identity/identity-access-service services/d07-identity/identity-access-service
COPY --from=portal /workspace/apps/auth-portal/dist apps/auth-portal/dist
RUN chmod +x gradlew && ./gradlew :identity-access-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app app
COPY --from=builder /workspace/services/d07-identity/identity-access-service/build/libs/app.jar app.jar
USER 10001:10001
EXPOSE 8097
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
