@file:WithArtifact("org.jsoup:jsoup:1.21.1")

import org.jsoup.Jsoup
import build.kotlin.withartifact.WithArtifact

fun jsoupTest(): Int {
    Jsoup.parse("<p>Hello</p>")
    return 1
}
