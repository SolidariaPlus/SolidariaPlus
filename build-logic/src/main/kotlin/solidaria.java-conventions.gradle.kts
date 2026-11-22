// Convenciones comunes a todo proyecto Java del monorepo:
// toolchain Java 21, JUnit 5, JaCoCo, Spotless (Palantir Java Format) y Checkstyle.
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    jacoco
    checkstyle
    id("com.diffplug.spotless")
}

val libs = the<LibrariesForLibs>()

group = "com.solidariaplus"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

// ---- Formato: `./gradlew spotlessApply` corrige, `spotlessCheck` (en `check`) valida ----
spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat(libs.versions.palantir.java.format.get())
        removeUnusedImports()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

// ---- Análisis estático: reglas en config/checkstyle/checkstyle.xml (raíz del repo) ----
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory = rootProject.layout.projectDirectory.dir("config/checkstyle")
    isIgnoreFailures = false
    maxWarnings = 0
}
