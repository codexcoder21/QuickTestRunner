package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.workspace
import kotlin.test.*
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class PackageWarningTest {
    @Test
    fun warningIfNoPackage() {
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val originalErr = System.err
        val buffer = ByteArrayOutputStream()
        System.setErr(PrintStream(buffer))
        try {
            QuickTestRunner()
                .workspace(root)
                .run()
        } finally {
            System.setErr(originalErr)
        }
        val output = buffer.toString()
        val testFile = File(root, "test.kts").absolutePath
        assertTrue(output.contains("warning:"), "Expected warning to be printed")
        assertTrue(output.contains(testFile), "Warning should include absolute file path")
    }
}
