package community.kotlin.test.quicktest

import kotlin.test.*
import java.io.File

class QuickTestRunnerPublicApiTest {
    @Test
    fun builderExecutesTests() {
        val dir = createTempDir(prefix = "qtr")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            fun passing() {}
            fun failing() { throw RuntimeException("boom") }
            """.trimIndent()
        )

        val results = QuickTestRunBuilder().directory(dir).run()
        assertEquals(2, results.results.size)
        val pass = results.results.first { it.function == "passing" }
        val fail = results.results.first { it.function == "failing" }
        assertTrue(pass.success)
        assertFalse(fail.success)
    }

    @Test
    fun logFilesAreWritten() {
        val dir = createTempDir(prefix = "qtr")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            fun passing() {}
            fun failing() { throw RuntimeException("boom") }
            """.trimIndent()
        )

        val xmlLog = File(dir, "log.xml")
        QuickTestRunBuilder().directory(dir).logFile(xmlLog).run()
        val xmlContent = xmlLog.readText()
        assertTrue(xmlContent.contains("<test") && xmlContent.contains("stacktrace"))

        val htmlLog = File(dir, "log.html")
        QuickTestRunBuilder().directory(dir).logFile(htmlLog).run()
        val htmlContent = htmlLog.readText()
        assertTrue(htmlContent.contains("<table>") && htmlContent.contains("RuntimeException"))
    }
}
