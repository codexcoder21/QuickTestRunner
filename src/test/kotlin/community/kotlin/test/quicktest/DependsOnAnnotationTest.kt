package community.kotlin.test.quicktest

import kotlin.test.*
import java.io.File

class DependsOnAnnotationTest {
    @Test
    fun annotationIsRepeatableAndAccessible() {
        val dir = createTempDir(prefix = "qtrdep")
        val testFile = File(dir, "quicktest.kts")
        testFile.writeText(
            """
            @file:DependsOn("ruleA")
            @file:DependsOn("ruleB")

            import community.kotlin.test.quicktest.DependsOn

            fun sample() {}
            """.trimIndent()
        )

        val results = QuickTestRunner().directory(dir).run()
        assertEquals(1, results.results.size)
        assertTrue(results.results.first().success)
    }
}
