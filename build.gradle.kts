plugins {
    kotlin("jvm") version "2.2.0"
}

group = "community.kotlin.test.quicktest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
