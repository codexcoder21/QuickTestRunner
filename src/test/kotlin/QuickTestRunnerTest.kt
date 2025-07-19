package org.example

import kotlin.test.*
import java.io.File

class QuickTestRunnerTest {

    @Test
    fun runnerExecutesTests() {
        val dir = createTempDir(prefix = "qtr")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            fun passing() {}
            fun failing() { throw RuntimeException("boom") }
            """.trimIndent()
        )

        val results = runTests(dir)
        assertEquals(2, results.size)
        val pass = results.first { it.function == "passing" }
        val fail = results.first { it.function == "failing" }
        assertTrue(pass.success)
        assertFalse(fail.success)
    }
}
