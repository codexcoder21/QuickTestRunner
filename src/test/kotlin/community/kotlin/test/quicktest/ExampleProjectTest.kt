package org.example

import community.kotlin.test.quicktest.QuickTestRunner
import community.kotlin.test.quicktest.workspace
import kotlin.test.*
import java.io.File

class ExampleProjectTest {
    @Test
    fun quickTestsPass() {
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val results = QuickTestRunner()
            .workspace(root)
            .run()
        assertEquals(2, results.results.size)
        assertTrue(results.failed().isEmpty(), "All quick tests should pass")
    }
}
