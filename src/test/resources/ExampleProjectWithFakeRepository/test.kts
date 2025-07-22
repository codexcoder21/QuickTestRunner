@file:WithRepository("https://fake.repo.example")
@file:WithArtifact("org.jsoup:jsoup:1.21.1")

import org.jsoup.Jsoup
import build.kotlin.withartifact.WithArtifact
import build.kotlin.withartifact.WithRepository

fun jsoupTest() {
    Jsoup.parse("<p>Hello</p>")
}
