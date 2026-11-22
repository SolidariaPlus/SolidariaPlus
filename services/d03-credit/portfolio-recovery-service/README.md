# portfolio-recovery-service (MS-07)

- Bounded context: `d03-credit`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.credit.portfoliorecovery`
- Puerto local: `8087`

## Desarrollo local

```bash
./gradlew :portfolio-recovery-service:bootRun   # levantar el servicio
./gradlew :portfolio-recovery-service:test      # tests + reporte JaCoCo
./gradlew :portfolio-recovery-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8087/actuator/health>
