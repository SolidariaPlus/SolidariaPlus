# identity-access-service (MS-17)

- Bounded context: `d07-identity`
- Stack: Java 21 · Spring Boot 4
- Paquete base: `com.solidariaplus.identity.identityaccess`
- Puerto local: `8097`

## Desarrollo local

```bash
./gradlew :identity-access-service:bootRun   # levantar el servicio
./gradlew :identity-access-service:test      # tests + reporte JaCoCo
./gradlew :identity-access-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8097/actuator/health>
