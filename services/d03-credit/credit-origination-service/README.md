# credit-origination-service (MS-05)

- Bounded context: `d03-credit`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.credit.creditorigination`
- Puerto local: `8085`

## Desarrollo local

```bash
./gradlew :credit-origination-service:bootRun   # levantar el servicio
./gradlew :credit-origination-service:test      # tests + reporte JaCoCo
./gradlew :credit-origination-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8085/actuator/health>
