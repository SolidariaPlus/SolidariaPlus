# vault-reconciliation-service (MS-09)

- Bounded context: `d04-teller-treasury`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.tellertreasury.vaultreconciliation`
- Puerto local: `8089`

## Desarrollo local

```bash
./gradlew :vault-reconciliation-service:bootRun   # levantar el servicio
./gradlew :vault-reconciliation-service:test      # tests + reporte JaCoCo
./gradlew :vault-reconciliation-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8089/actuator/health>
