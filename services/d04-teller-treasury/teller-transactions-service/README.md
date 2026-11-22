# teller-transactions-service (MS-08)

- Bounded context: `d04-teller-treasury`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.tellertreasury.tellertransactions`
- Puerto local: `8088`

## Desarrollo local

```bash
./gradlew :teller-transactions-service:bootRun   # levantar el servicio
./gradlew :teller-transactions-service:test      # tests + reporte JaCoCo
./gradlew :teller-transactions-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8088/actuator/health>
