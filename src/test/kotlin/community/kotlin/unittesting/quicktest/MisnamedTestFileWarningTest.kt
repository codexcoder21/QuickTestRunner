package org.example

import community.kotlin.unittesting.quicktest.MisnamedTestFileEffect
import community.kotlin.unittesting.quicktest.QuickTestRunner
import community.kotlin.unittesting.quicktest.workspace
import kotlinx.algebraiceffects.Effective
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import java.io.File

class MisnamedTestFileWarningTest {
    @Test
    fun warningForMisnamedFile() = runBlocking {
        val root = File("src/test/resources/ExampleProjectWithMisnamedTestFile")
        val channel = Channel<String>(1)
        Effective<Unit> {
            handler { e: MisnamedTestFileEffect ->
                channel.trySend(e.message!!)
            }
            QuickTestRunner()
                .workspace(root)
                .run()
        }
        val output = withTimeout(1000) { channel.receive() }
        val testFile = File(root, "tests.kts").absolutePath
        assertTrue(output.contains("test.kts"), "Message should mention expected file name")
        assertTrue(output.contains(testFile), "Message should include absolute file path")
    }
}
