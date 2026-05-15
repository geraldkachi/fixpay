// shared is a plain Java library — no Spring Boot plugin, no fat jar
plugins {
    `java-library`
}

dependencies {
    implementation(libs.jackson.annotations)
    api(libs.pf4j)                    // plugin authors need ExtensionPoint at compile time
}
