plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
    }
}

dependencies {
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.cloud.circuitbreaker)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.webflux)
    implementation(libs.spring.boot.oauth2.resource.server)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    testImplementation("org.springframework.security:spring-security-test")
}
