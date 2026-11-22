# immutable-audit-ledger-service (MS-15)

- Bounded context: `d06-aml-audit`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.amlaudit.immutableauditledger`
- Puerto local: `8095`

## Desarrollo local

```bash
./gradlew :immutable-audit-ledger-service:bootRun   # levantar el servicio
./gradlew :immutable-audit-ledger-service:test      # tests + reporte JaCoCo
./gradlew :immutable-audit-ledger-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8095/actuator/health>
