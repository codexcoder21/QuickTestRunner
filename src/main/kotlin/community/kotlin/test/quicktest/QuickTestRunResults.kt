package community.kotlin.test.quicktest

import java.io.File

/** Result of running quick tests. */
data class TestResult(val file: File, val function: String, val success: Boolean, val error: Throwable?)

class QuickTestRunResults(val results: List<TestResult>) {
    fun passed(): List<TestResult> = results.filter { it.success }
    fun failed(): List<TestResult> = results.filter { !it.success }
}
