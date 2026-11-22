# ml-fraud-aml-service (MS-14)

- Bounded context: `d06-aml-audit`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.amlaudit.mlfraudaml`
- Puerto local: `8094`

## Desarrollo local

```bash
./gradlew :ml-fraud-aml-service:bootRun   # levantar el servicio
./gradlew :ml-fraud-aml-service:test      # tests + reporte JaCoCo
./gradlew :ml-fraud-aml-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8094/actuator/health>
