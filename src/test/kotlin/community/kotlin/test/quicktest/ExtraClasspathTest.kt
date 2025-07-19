package community.kotlin.test.quicktest

import kotlin.test.*
import java.io.File

class ExtraClasspathTest {
    @Test
    fun runnerUsesExtraClasspath() {
        val jar = createMathJar()
        val dir = createTempDir(prefix = "qtrcp")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            fun addTest() { kotlin.test.assertEquals(5, MathOps.add(2,3)) }
            fun subTest() { kotlin.test.assertEquals(1, MathOps.subtract(3,2)) }
            fun mulTest() { kotlin.test.assertEquals(6, MathOps.multiply(2,3)) }
            fun divTest() { kotlin.test.assertEquals(2, MathOps.divide(6,3)) }
            """.trimIndent()
        )
        val cp = System.getProperty("java.class.path") + File.pathSeparator + jar.absolutePath
        val results = QuickTestRunner()
            .directory(dir)
            .classpath(cp)
            .run()
        assertTrue(results.failed().isEmpty(), "All tests should pass")
    }
}
