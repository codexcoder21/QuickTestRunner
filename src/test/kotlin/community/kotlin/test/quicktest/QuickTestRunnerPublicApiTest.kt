package community.kotlin.test.quicktest

import kotlin.test.*
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class QuickTestRunnerPublicApiTest {
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

        val results = QuickTestRunner().directory(dir).run()
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
        QuickTestRunner().directory(dir).logFile(xmlLog).run()
        val xmlContent = xmlLog.readText()
        assertTrue(xmlContent.contains("<test") && xmlContent.contains("stacktrace"))

        val htmlLog = File(dir, "log.html")
        QuickTestRunner().directory(dir).logFile(htmlLog).run()
        val htmlContent = htmlLog.readText()
        assertTrue(htmlContent.contains("<table>") && htmlContent.contains("RuntimeException"))
    }

    @Test
    fun customClasspathIsUsed() {
        val libDir = createTempDir(prefix = "lib")
        val srcFile = File(libDir, "Helper.kts")
        srcFile.writeText(
            """
            package helper
            object Helper { fun value() = 42 }
            """.trimIndent()
        )
        val outDir = File(libDir, "out").apply { mkdirs() }
        QuickTestUtils.compileQuickTest(FileSystem.SYSTEM, srcFile.toPath().toOkioPath(), FileSystem.SYSTEM, outDir.toPath().toOkioPath())
        val jarFile = File(libDir, "helper.jar")
        java.util.jar.JarOutputStream(jarFile.outputStream()).use { jar ->
            outDir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { cl ->
                val name = outDir.toPath().relativize(cl.toPath()).toString().replace(File.separatorChar, '/')
                jar.putNextEntry(java.util.jar.JarEntry(name))
                cl.inputStream().copyTo(jar)
                jar.closeEntry()
            }
        }

        val testDir = createTempDir(prefix = "qtr")
        val testFile = File(testDir, "quicktest.kts")
        testFile.writeText(
            """
            @file:community.kotlin.test.quicktest.DependsOn("${jarFile.absolutePath}")
            import helper.Helper
            fun usesHelper() { if (Helper.value() != 42) throw RuntimeException("bad") }
            """.trimIndent()
        )

        val results = QuickTestRunner().directory(testDir).run()
        assertEquals(1, results.results.size)
        assertTrue(results.results.first().success)
    }
}
