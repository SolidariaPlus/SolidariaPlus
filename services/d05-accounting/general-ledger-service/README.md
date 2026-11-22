# general-ledger-service (MS-11)

- Bounded context: `d05-accounting`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.accounting.generalledger`
- Puerto local: `8091`

## Desarrollo local

```bash
./gradlew :general-ledger-service:bootRun   # levantar el servicio
./gradlew :general-ledger-service:test      # tests + reporte JaCoCo
./gradlew :general-ledger-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8091/actuator/health>
