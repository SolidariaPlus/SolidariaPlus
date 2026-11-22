# smart-accounting-rules-service (MS-12)

- Bounded context: `d05-accounting`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.accounting.smartaccountingrules`
- Puerto local: `8092`

## Desarrollo local

```bash
./gradlew :smart-accounting-rules-service:bootRun   # levantar el servicio
./gradlew :smart-accounting-rules-service:test      # tests + reporte JaCoCo
./gradlew :smart-accounting-rules-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8092/actuator/health>
