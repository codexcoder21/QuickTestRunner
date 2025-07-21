@file:WithArtifact("org.example.buildMaven()")

import org.example.add
import org.example.subtract
import build.kotlin.withartifact.WithArtifact

fun addTest() { if(add(2,3) != 5) throw Error("Addition broken") }
fun subTest() { if(subtract(2,3) != -1) throw Error("Subtraction broken") }
