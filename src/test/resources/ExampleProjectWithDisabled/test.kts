@file:WithArtifact("org.jsoup:jsoup:1.21.1")

import org.jsoup.Jsoup
import build.kotlin.withartifact.WithArtifact
import build.kotlin.annotations.Disabled

fun enabledTest() {
    Jsoup.parse("<p>Hello</p>")
}

@Disabled("not ready")
fun disabledTest() {
    throw Error("Should not run")
}
