import java.net.URI

plugins {
    kotlin("jvm") version "2.2.0"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
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

fun parseProxy(env: String): Pair<String, String>? {
    val value = System.getenv(env) ?: System.getenv(env.lowercase()) ?: return null
    val formatted = if ("://" in value) value else "http://$value"
    return try {
        val uri = URI(formatted)
        val host = uri.host ?: return null
        val port = if (uri.port == -1) "80" else uri.port.toString()
        host to port
    } catch (_: Exception) {
        null
    }
}

val httpProxy = parseProxy("HTTP_PROXY")
val httpsProxy = parseProxy("HTTPS_PROXY")

fun JavaForkOptions.applyProxy() {
    httpProxy?.let { (h, p) ->
        systemProperty("http.proxyHost", h)
        systemProperty("http.proxyPort", p)
    }
    httpsProxy?.let { (h, p) ->
        systemProperty("https.proxyHost", h)
        systemProperty("https.proxyPort", p)
    }
}

tasks.withType<Test>().configureEach {
    applyProxy()
}

tasks.withType<JavaExec>().configureEach {
    applyProxy()
}
