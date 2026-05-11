plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
    }
}

dependencies {
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.cloud.circuitbreaker)
    implementation(libs.spring.boot.actuator)
    // Phase 1: uncomment to validate JWTs at the gateway
    // implementation(libs.spring.boot.webflux)
    // implementation(libs.spring.boot.oauth2.resource.server)
}
