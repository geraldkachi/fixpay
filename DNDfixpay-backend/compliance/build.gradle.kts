plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)

    // Phase 9: uncomment when wiring external AML/CFT adapters
    // implementation(libs.nats.jnats)
    // implementation(libs.spring.boot.data.jpa)
    // implementation(libs.postgresql)
    // runtimeOnly(libs.postgresql)
}
