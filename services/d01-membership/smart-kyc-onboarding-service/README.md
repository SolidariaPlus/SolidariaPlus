# smart-kyc-onboarding-service (MS-02)

- Bounded context: `d01-membership`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.membership.smartkyconboarding`
- Puerto local: `8082`

## Desarrollo local

```bash
./gradlew :smart-kyc-onboarding-service:bootRun   # levantar el servicio
./gradlew :smart-kyc-onboarding-service:test      # tests + reporte JaCoCo
./gradlew :smart-kyc-onboarding-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8082/actuator/health>
