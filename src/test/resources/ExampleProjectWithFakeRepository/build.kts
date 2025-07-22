@KotlinBuildScript("https://tools.kotlin.build/")
package org.example

import java.io.File
import build.kotlin.jvm.*

val dependencies = resolveDependencies(
    MavenPrebuilt("org.jetbrains.kotlin:kotlin-stdlib:1.8.22"),
    MavenPrebuilt("org.jetbrains.kotlin:kotlin-stdlib-common:1.8.22"),
    MavenPrebuilt("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22"),
    MavenPrebuilt("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22"),
)

fun buildMaven(): File {
    return buildSimpleKotlinMavenArtifact(
        coordinates="org.example:ExampleTestProjeectWithBuildScriptAndTests:0.0.1",
        src=File("src"),
        compileDependencies=dependencies
    )
}
