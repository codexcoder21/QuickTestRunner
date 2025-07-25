@file:WithArtifact("org.example.buildMaven()")
@file:WithArtifact("org.jsoup:jsoup:1.21.1")
@file:WithArtifact("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")

import org.example.add
import org.example.subtract
import org.jsoup.Jsoup
import build.kotlin.withartifact.WithArtifact

fun addTest() { if(add(2,3) != 5) throw Error("Addition broken") }
fun subTest() { if(subtract(2,3) != -1) throw Error("Subtraction broken") }

fun jsoupTest() {
    val doc = Jsoup.parse("<p>Hello</p>")
    println(doc.text())
}
