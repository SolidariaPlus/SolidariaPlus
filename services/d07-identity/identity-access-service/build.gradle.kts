plugins {
    id("solidaria.spring-boot-service")
}

dependencies {
    implementation(libs.spring.boot.starter.oauth2.authorization.server)
    implementation(libs.spring.boot.starter.kafka)
}
