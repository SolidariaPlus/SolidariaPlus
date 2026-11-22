# ml-credit-scoring-service (MS-06)

- Bounded context: `d03-credit`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.credit.mlcreditscoring`
- Puerto local: `8086`

## Desarrollo local

```bash
./gradlew :ml-credit-scoring-service:bootRun   # levantar el servicio
./gradlew :ml-credit-scoring-service:test      # tests + reporte JaCoCo
./gradlew :ml-credit-scoring-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8086/actuator/health>
