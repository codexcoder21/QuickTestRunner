package org.example

import kotlin.test.*
import java.io.File

class HelpOptionTest {
    @Test
    fun helpOptionShowsUsage() {
        val jar = File("build/libs/QuickTestRunner-1.0-SNAPSHOT-all.jar")
        assertTrue(jar.exists(), "Fat jar should be built")

        val process = ProcessBuilder("java", "-jar", jar.absolutePath, "--help")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()

        val cleaned = output.lines()
            .filterNot { it.startsWith("warning:") || it.isBlank() }
            .joinToString("\n").trim()

        assertEquals(0, exit)
        assertTrue(cleaned.contains("usage"), "Should print usage header")
        assertTrue(cleaned.contains("--directory"), "Should mention directory option")
    }
}
