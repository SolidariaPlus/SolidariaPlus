// Microservicio Spring Boot desplegable (services/<dominio>/<servicio>).
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("solidaria.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = the<LibrariesForLibs>()

// BOMs adicionales al de Spring Boot: cada servicio declara los módulos que necesite
// (p.ej. libs.spring.cloud.aws.starter.sqs, libs.bundles.resilience, libs.flyway.core) sin versión.
dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.aws.bom.get().toString())
        mavenBom(libs.resilience4j.bom.get().toString())
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.bundles.observability)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Nombre de artefacto fijo para que el Dockerfile sea idéntico en todos los servicios
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "app.jar"
}

tasks.named<Jar>("jar") {
    enabled = false
}
