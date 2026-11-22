# regulatory-supervision-service (MS-16)

- Bounded context: `d06-aml-audit`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.amlaudit.regulatorysupervision`
- Puerto local: `8096`

## Desarrollo local

```bash
./gradlew :regulatory-supervision-service:bootRun   # levantar el servicio
./gradlew :regulatory-supervision-service:test      # tests + reporte JaCoCo
./gradlew :regulatory-supervision-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8096/actuator/health>
