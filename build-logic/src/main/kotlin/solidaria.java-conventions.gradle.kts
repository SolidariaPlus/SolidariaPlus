// Shared conventions for every Java project in the monorepo:
// Java 21 toolchain, JUnit 6, JaCoCo, ArchUnit architecture tests, Spotless (Palantir Java Format) and Checkstyle.
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

val architectureTag = "architecture"

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags(architectureTag)
    }
    finalizedBy(tasks.named("jacocoTestReport"))
}

val architectureTest =
    tasks.register<Test>("architectureTest") {
        description = "Runs the ArchUnit architecture rules."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags(architectureTag)
        }
        shouldRunAfter(tasks.named("test"))
    }

tasks.named("check") {
    dependsOn(architectureTest)
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

// ---- Formatting: `./gradlew spotlessApply` fixes, `spotlessCheck` (part of `check`) verifies ----
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

// ---- Static analysis: rules in config/checkstyle/checkstyle.xml (repository root) ----
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory = rootProject.layout.projectDirectory.dir("config/checkstyle")
    isIgnoreFailures = false
    maxWarnings = 0
}
