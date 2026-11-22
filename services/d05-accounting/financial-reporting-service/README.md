# financial-reporting-service (MS-13)

- Bounded context: `d05-accounting`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.accounting.financialreporting`
- Puerto local: `8093`

## Desarrollo local

```bash
./gradlew :financial-reporting-service:bootRun   # levantar el servicio
./gradlew :financial-reporting-service:test      # tests + reporte JaCoCo
./gradlew :financial-reporting-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8093/actuator/health>
