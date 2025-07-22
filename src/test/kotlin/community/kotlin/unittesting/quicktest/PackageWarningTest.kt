package org.example

import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.PackageWarningEffect
import community.kotlin.unittesting.quicktest.workspace
import kotlinx.algebraiceffects.Effective
import kotlinx.algebraiceffects.NotificationEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import java.io.File

class PackageWarningTest {
    @Test
    fun warningIfNoPackage() = runBlocking {
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val channel = Channel<String>(1)
        Effective<Unit> {
            handler { e: NotificationEffect ->
                if (e is PackageWarningEffect) {
                    channel.trySend(e.message!!)
                }
            }
            QuickTestRunner()
                .workspace(root)
                .run()
        }
        val output = withTimeout(1000) { channel.receive() }
        val testFile = File(root, "test.kts").absolutePath
        assertTrue(output.contains("warning:"), "Expected warning to be printed")
        assertTrue(output.contains(testFile), "Warning should include absolute file path")
    }
}
