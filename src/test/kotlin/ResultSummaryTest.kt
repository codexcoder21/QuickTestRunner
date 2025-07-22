package org.example

import kotlin.test.*
import java.io.File
import java.net.URI

class ResultSummaryTest {
    private val jar = File("build/libs/QuickTestRunner-1.0-SNAPSHOT-all.jar")

    private fun parseProxy(env: String): Pair<String, String>? {
        val value = System.getenv(env) ?: System.getenv(env.lowercase()) ?: return null
        val formatted = if ("://" in value) value else "http://$value"
        return try {
            val uri = URI(formatted)
            val host = uri.host ?: return null
            val port = if (uri.port == -1) "80" else uri.port.toString()
            host to port
        } catch (_: Exception) {
            null
        }
    }

    private fun proxyArgs(): List<String> {
        val args = mutableListOf<String>()
        parseProxy("HTTP_PROXY")?.let { (h, p) ->
            args += "-Dhttp.proxyHost=$h"
            args += "-Dhttp.proxyPort=$p"
        }
        parseProxy("HTTPS_PROXY")?.let { (h, p) ->
            args += "-Dhttps.proxyHost=$h"
            args += "-Dhttps.proxyPort=$p"
        }
        return args
    }

    @Test
    fun allTestsPassSummary() {
        assertTrue(jar.exists(), "Fat jar should be built")
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val cmd = mutableListOf("java") + proxyArgs() + listOf("-jar", jar.absolutePath, "--workspace", root.absolutePath)
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        assertEquals(0, exit)
        assertTrue(output.contains("ALL TESTS PASSED"), "Should report all tests passed")
    }

    @Test
    fun failingTestsSummaryAndExitCode() {
        assertTrue(jar.exists(), "Fat jar should be built")
        val root = File("src/test/resources/ExampleProjectWithFakeRepository")
        val cmd = mutableListOf("java") + proxyArgs() + listOf("-jar", jar.absolutePath, "--workspace", root.absolutePath)
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        assertNotEquals(0, exit)
        assertTrue(output.contains("TEST FAILURES"), "Should report failures")
    }

    @Test
    fun verbosePrintsPassingTests() {
        assertTrue(jar.exists(), "Fat jar should be built")
        val root = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests")
        val cmd = mutableListOf("java") + proxyArgs() + listOf("-jar", jar.absolutePath, "--workspace", root.absolutePath, "--verbose")
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val passedCount = output.lines().count { it.trim().startsWith("PASSED") }
        assertEquals(3, passedCount, "Verbose output should include all passing tests")
    }

    @Test
    fun disabledTestsSummaryAndOutput() {
        assertTrue(jar.exists(), "Fat jar should be built")
        val root = File("src/test/resources/ExampleProjectWithDisabled")
        val cmd = mutableListOf("java") + proxyArgs() + listOf("-jar", jar.absolutePath, "--workspace", root.absolutePath)
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        assertEquals(0, exit)
        assertTrue(output.contains("ALL TESTS PASSED"))
        assertTrue(output.contains("1 unit tests disabled"))
        assertFalse(output.contains("disabledTest"), "Disabled test name should not be printed")
    }

    @Test
    fun verbosePrintsDisabledTests() {
        assertTrue(jar.exists(), "Fat jar should be built")
        val root = File("src/test/resources/ExampleProjectWithDisabled")
        val cmd = mutableListOf("java") + proxyArgs() + listOf("-jar", jar.absolutePath, "--workspace", root.absolutePath, "--verbose")
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        assertTrue(output.lines().any { it.trim() == "DISABLED disabledTest" }, "Verbose output should include disabled tests")
    }
}
