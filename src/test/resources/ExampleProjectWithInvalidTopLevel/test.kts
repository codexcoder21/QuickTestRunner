@file:WithArtifact("org.jsoup:jsoup:1.21.1")

import org.jsoup.Jsoup
import build.kotlin.withartifact.WithArtifact

val number = 5

fun exampleTest() {
    Jsoup.parse("<p>Hi</p>")
}

class ExtraClass
