package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.TestStatus
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File

class InvalidTopLevelTest {
    @Test
    fun nonFunctionMembersFail() {
        val root = File("src/test/resources/ExampleProjectWithInvalidTopLevel")
        val results = QuickTestRunner()
            .workspace(root)
            .run()
        val failures = results.getResults(TestStatus.FAILURE)
        assertTrue(failures.isNotEmpty(), "Expected failure due to invalid top-level declarations")
    }
}
