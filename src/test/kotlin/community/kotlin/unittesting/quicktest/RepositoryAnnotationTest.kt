package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File

class RepositoryAnnotationTest {

    @Test
    fun failingRepositoryListsRepositoriesInError() {
        val root = File("src/test/resources/ExampleProjectWithFakeRepository")
        val error = assertFailsWith<RuntimeException> {
            QuickTestRunner()
                .workspace(root)
                .run()
        }
        assertTrue(error.message!!.contains("org.jsoup:jsoup:1.21.1"))
        assertTrue(error.message!!.contains("https://fake.repo.example"))
    }

    @Test
    fun multipleRepositoriesAreCollected() {
        val root = File("src/test/resources/ExampleProjectWithMultipleRepositories")
        val error = assertFailsWith<RuntimeException> {
            QuickTestRunner()
                .workspace(root)
                .run()
        }
        assertTrue(error.message!!.contains("https://fake.repo.one"))
        assertTrue(error.message!!.contains("https://fake.repo.two"))
    }

    @Test
    fun defaultsUsedWhenNoRepositorySpecified() {
        val root = File("src/test/resources/ExampleProjectWithDefaultRepositories")
        val error = assertFailsWith<RuntimeException> {
            QuickTestRunner()
                .workspace(root)
                .run()
        }
        assertTrue(error.message!!.contains("http://kotlin.directory"))
        assertTrue(error.message!!.contains("https://repo1.maven.org/maven2/"))
    }
}

