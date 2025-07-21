plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "community.kotlin.unittesting.quicktest"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("http://kotlin.directory")
        metadataSources {
            mavenPom()
            artifact()
        }
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

dependencies {
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.squareup.okio:okio:3.6.0")
    implementation("kompile-cli:kompile-cli:0.0.1")
    implementation("community.kotlin.psi.annotationutils:community-kotlin-psi-annotationutils:0.0.1")
    implementation("build.kotlin.withartifact:build-kotlin-withartifact:0.0.1")
    implementation("io.get-coursier:interface:1.0.28")
    testImplementation(kotlin("test"))
    testImplementation("org.eclipse.jdt:ecj:3.33.0")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(fatJar)
}

tasks.named<Jar>("jar") {
    from("README.md")
}

application {
    mainClass.set("community.kotlin.unittesting.quicktest.QuickTestRunner")
}

val fatJar by tasks.registering(Jar::class) {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "community.kotlin.unittesting.quicktest.QuickTestRunner"
    }
    from(sourceSets.main.get().output)
    from("README.md")
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
