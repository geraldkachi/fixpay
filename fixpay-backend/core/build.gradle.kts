plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)

    // Phase 1+: uncomment progressively
    // implementation(libs.spring.boot.data.jpa)
    // implementation(libs.spring.boot.data.redis)
    // implementation(libs.spring.boot.oauth2.resource.server)
    // implementation(libs.postgresql)
    // runtimeOnly(libs.postgresql)
    // implementation(libs.flyway.core)
    // implementation(libs.flyway.postgresql)
    // implementation(libs.nats.jnats)
}
