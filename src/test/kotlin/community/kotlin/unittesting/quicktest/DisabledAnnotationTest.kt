package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.TestStatus
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File

class DisabledAnnotationTest {
    @Test
    fun disabledTestsAreNotRun() {
        val root = File("src/test/resources/ExampleProjectWithDisabled")
        val results = QuickTestRunner()
            .workspace(root)
            .run()
        assertEquals(2, results.results.size)
        assertTrue(results.getResults(TestStatus.FAILURE).isEmpty(), "No failures expected")
        assertEquals(1, results.getResults(TestStatus.DISABLED).size, "One test should be disabled")
        assertEquals(1, results.getResults(TestStatus.SUCCESS).size, "One test should run")
    }
}
