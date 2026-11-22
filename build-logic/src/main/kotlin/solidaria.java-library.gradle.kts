// Shared Java libraries (libs/*).
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("solidaria.java-conventions")
    `java-library`
}

val libs = the<LibrariesForLibs>()

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
