plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.spotless.gradle.plugin)
    // Exposes the type-safe `libs` accessor inside the precompiled script plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
