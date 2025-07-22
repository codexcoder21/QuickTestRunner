package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.TestStatus
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File

class ExampleProjectTest {
    @Test
    fun quickTestsPass() {
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val results = QuickTestRunner()
            .workspace(root)
            .run()
        assertEquals(3, results.results.size)
        assertTrue(results.getResults(TestStatus.FAILURE).isEmpty(), "All quick tests should pass")
    }
}
