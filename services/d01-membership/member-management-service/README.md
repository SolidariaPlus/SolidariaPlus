# member-management-service (MS-01)

- Bounded context: `d01-membership`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.membership.membermanagement`
- Puerto local: `8081`

## Desarrollo local

```bash
./gradlew :member-management-service:bootRun   # levantar el servicio
./gradlew :member-management-service:test      # tests + reporte JaCoCo
./gradlew :member-management-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8081/actuator/health>
