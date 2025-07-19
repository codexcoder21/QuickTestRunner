package org.example

import kotlin.test.*
import java.io.File

class FatJarTest {
    @Test
    fun fatJarRuns() {
        val dir = createTempDir(prefix = "qtrj")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            fun pass() {}
            fun fail() { throw RuntimeException("boom") }
            """.trimIndent()
        )

        val jar = File("build/libs/QuickTestRunner-1.0-SNAPSHOT-all.jar")
        assertTrue(jar.exists(), "Fat jar should be built")

        val process = ProcessBuilder("java", "-jar", jar.absolutePath, "--directory", dir.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()

        val cleaned = output.lines()
            .filterNot { it.startsWith("warning:") || it.isBlank() }
            .joinToString("\n").trim()

        val expected = """
            FAILED ${testFile.absolutePath}:fail -> boom
            PASSED ${testFile.absolutePath}:pass
        """.trimIndent()

        assertEquals(0, exit)
        assertEquals(expected, cleaned)
    }
}
