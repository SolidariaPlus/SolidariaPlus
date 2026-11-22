# interest-and-contributions-service (MS-04)

- Bounded context: `d02-savings`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.savings.interestandcontributions`
- Puerto local: `8084`

## Desarrollo local

```bash
./gradlew :interest-and-contributions-service:bootRun   # levantar el servicio
./gradlew :interest-and-contributions-service:test      # tests + reporte JaCoCo
./gradlew :interest-and-contributions-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8084/actuator/health>
