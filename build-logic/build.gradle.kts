plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.spotless.gradle.plugin)
    // Expone el accessor type-safe `libs` dentro de los precompiled script plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
