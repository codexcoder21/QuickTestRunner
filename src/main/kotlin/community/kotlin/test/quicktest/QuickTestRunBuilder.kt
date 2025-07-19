package community.kotlin.test.quicktest

import java.io.File

/** Builder style API for executing quick tests programmatically. */
class QuickTestRunBuilder {
    private var directory: File = File(".")
    private var logFile: File? = null

    fun directory(dir: File): QuickTestRunBuilder = apply { this.directory = dir }
    fun logFile(file: File): QuickTestRunBuilder = apply { this.logFile = file }

    fun run(): QuickTestRunResults {
        val results = QuickTestRunner.runTests(directory)
        logFile?.let { QuickTestUtils.writeResults(results, it) }
        return QuickTestRunResults(results)
    }
}
