# cash-liquidity-forecast-service (MS-10)

- Bounded context: `d04-teller-treasury`
- Stack: Java 21 · Spring Boot 3
- Paquete base: `com.solidariaplus.tellertreasury.cashliquidityforecast`
- Puerto local: `8090`

## Desarrollo local

```bash
./gradlew :cash-liquidity-forecast-service:bootRun   # levantar el servicio
./gradlew :cash-liquidity-forecast-service:test      # tests + reporte JaCoCo
./gradlew :cash-liquidity-forecast-service:bootJar   # build/libs/app.jar
```

Health check: <http://localhost:8090/actuator/health>
