// Deployable Spring Boot microservice (services/<domain>/<service>).
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("solidaria.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = the<LibrariesForLibs>()

// BOMs in addition to Spring Boot's: each service declares the modules it needs
// (e.g. libs.spring.cloud.aws.starter.sqs, libs.bundles.resilience, libs.spring.boot.starter.flyway) without a version.
dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.aws.bom.get().toString())
        mavenBom(libs.resilience4j.bom.get().toString())
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.bundles.observability)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Fixed artifact name so the Dockerfile is identical across services
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "app.jar"
}

tasks.named<Jar>("jar") {
    enabled = false
}
