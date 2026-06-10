plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.data.jpa)
    implementation(libs.spring.boot.oauth2.resource.server)
    implementation(libs.spring.boot.data.redis)
    implementation(libs.spring.boot.mail)
    implementation(libs.nats.jnats)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.spring.boot.aop)
    runtimeOnly(libs.postgresql)
    testImplementation("org.springframework.security:spring-security-test")
}
