rootProject.name = "solidariaplus"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

fun includeProjectsUnder(base: File, depth: Int) {
    val candidates = when (depth) {
        1 -> base.listFiles().orEmpty().filter { it.isDirectory }
        else -> base.listFiles().orEmpty().filter { it.isDirectory }
            .flatMap { it.listFiles().orEmpty().filter { d -> d.isDirectory } }
    }
    candidates
        .filter { File(it, "build.gradle.kts").isFile }
        .sortedBy { it.name }
        .forEach { dir ->
            include(":${dir.name}")
            project(":${dir.name}").projectDir = dir
        }
}

includeProjectsUnder(file("services"), depth = 2)
includeProjectsUnder(file("libs"), depth = 1)
