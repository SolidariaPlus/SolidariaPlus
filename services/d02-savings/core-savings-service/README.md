# core-savings-service (MS-03)

- Bounded context: `d02-savings`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.savings.coresavings`
- Puerto local: `8083`

## Desarrollo local

```bash
./gradlew :core-savings-service:bootRun   # levantar el servicio
./gradlew :core-savings-service:test      # tests + reporte JaCoCo
./gradlew :core-savings-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8083/actuator/health>
