plugins {
    kotlin("jvm") version "2.1.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.21")
    implementation("commons-cli:commons-cli:1.5.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
