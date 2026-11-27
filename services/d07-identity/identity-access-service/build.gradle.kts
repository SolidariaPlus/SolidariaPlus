plugins {
    id("solidaria.spring-boot-service")
}

dependencies {
    implementation(libs.spring.boot.starter.oauth2.authorization.server)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.ldap)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.spring.cloud.aws.starter.secrets.manager)
    implementation(libs.spring.cloud.aws.starter.parameter.store)
    implementation(libs.aws.encryption.sdk.java)
    implementation(libs.aws.cryptographic.material.providers)
    implementation(libs.aws.sdk.kms)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.sentry.spring.boot.starter)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.testcontainers.postgresql)
}

val domainCoverage =
    tasks.register<JacocoCoverageVerification>("domainCoverage") {
        description = "Requires 80 % line coverage of the domain layer (RNF-MNT-02)."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(tasks.named("test"))
        executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))
        sourceDirectories.setFrom(
            sourceSets.main
                .get()
                .allJava.srcDirs,
        )
        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.asFileTree.matching {
                include("**/identityaccess/domain/**")
            },
        )
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

tasks.named("check") {
    dependsOn(domainCoverage)
}
