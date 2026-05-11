plugins {
    alias(libs.plugins.springBoot)        apply false
    alias(libs.plugins.springDependencyManagement) apply false
}

allprojects {
    group   = "ng.fixpay"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.springframework.boot:spring-boot-starter-test:3.4.1")
        "testImplementation"("org.testcontainers:junit-jupiter:1.20.4")
        "testImplementation"("org.testcontainers:postgresql:1.20.4")
    }
}
