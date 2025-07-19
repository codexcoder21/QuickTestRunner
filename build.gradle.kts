plugins {
    kotlin("jvm") version "2.2.0"
    application
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
    implementation("com.squareup.okio:okio:3.6.0")
    testImplementation(kotlin("test"))
    testImplementation("org.eclipse.jdt:ecj:3.33.0")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(fatJar)
}

application {
    mainClass.set("community.kotlin.test.quicktest.QuickTestRunner")
}

val fatJar by tasks.registering(Jar::class) {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "community.kotlin.test.quicktest.QuickTestRunner"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
