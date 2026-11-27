plugins {
    id("solidaria.java-library")
}

dependencies {
    // Import the Spring Boot BOM so spring-security / spring-data versions are consistent.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.2.0"))

    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
