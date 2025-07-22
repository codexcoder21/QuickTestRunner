package community.kotlin.unittesting.quicktest

import okio.Path

/** Status of a single test execution. */
enum class TestStatus {
    SUCCESS,
    FAILURE,
    /** A unit test which exists and can be reenabled, but which is currently set to not run. */
    DISABLED,
    /** The test was forcibly terminated due to external factors. */
    ABORTED,
    /** The unit test is running but has not yet completed. */
    RUNNING
}

/** Result of running quick tests. */
data class TestResult(val file: Path, val function: String, val status: TestStatus, val error: Throwable?)

class QuickTestRunResults(val results: List<TestResult>) {
    fun getResults(status: TestStatus? = null): List<TestResult> =
        status?.let { results.filter { r -> r.status == it } } ?: results
}
