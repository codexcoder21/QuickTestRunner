package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.TestStatus
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File

class RepositoryAnnotationTest {

    @Test
    fun failingRepositoryListsRepositoriesInError() {
        val root = File("src/test/resources/ExampleProjectWithFakeRepository")
        val results = QuickTestRunner()
            .workspace(root)
            .run()

        val failures = results.getResults(TestStatus.FAILURE)
        assertTrue(failures.isNotEmpty(), "Expected the quick test to fail")
        val message = failures.first().error?.message

        assertNotNull(message)
        assertTrue(message!!.contains("org.jsoup:jsoup:1.21.1"))
        assertTrue(message.contains("https://fake.repo.example"))
    }

    @Test
    fun multipleRepositoriesAreCollected() {
        val root = File("src/test/resources/ExampleProjectWithMultipleRepositories")
        val results = QuickTestRunner()
            .workspace(root)
            .run()

        val failures = results.getResults(TestStatus.FAILURE)
        assertTrue(failures.isNotEmpty(), "Expected the quick test to fail")
        val message = failures.first().error?.message

        assertNotNull(message)
        assertTrue(message!!.contains("https://fake.repo.one"))
        assertTrue(message.contains("https://fake.repo.two"))
    }

    @Test
    fun defaultsUsedWhenNoRepositorySpecified() {
        val root = File("src/test/resources/ExampleProjectWithDefaultRepositories")
        val results = QuickTestRunner()
            .workspace(root)
            .run()

        val failures = results.getResults(TestStatus.FAILURE)
        assertTrue(failures.isEmpty(), "Expected no failures")
        assertTrue(results.getResults(TestStatus.SUCCESS).size == 1, "Expected the quick test to pass")
    }
}

