package community.kotlin.test.quicktest

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

/** Entry point and core runner for quick tests. */
class QuickTestRunner {
    private var directory: File = File(".")
    private var logFile: File? = null

    fun directory(dir: File): QuickTestRunner = apply { this.directory = dir }
    fun logFile(file: File): QuickTestRunner = apply { this.logFile = file }

    fun run(): QuickTestRunResults {
        val results = runTests(directory)
        logFile?.let { QuickTestUtils.writeResults(results, it) }
        return QuickTestRunResults(results)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options().apply {
                addOption(Option.builder().longOpt("directory").hasArg().desc("Directory to scan").build())
                addOption(Option.builder().longOpt("log").hasArg().desc("Log file to dump results").build())
            }
            val cmd = DefaultParser().parse(options, args)
            val dirPath = cmd.getOptionValue("directory", ".")
            val logPath = cmd.getOptionValue("log")
            val runner = QuickTestRunner().directory(File(dirPath))
            if (logPath != null) runner.logFile(File(logPath))
            val results = runner.run()
            results.results.forEach { result ->
                if (result.success) {
                    println("PASSED ${result.file}:${result.function}")
                } else {
                    println("FAILED ${result.file}:${result.function} -> ${result.error?.message}")
                }
            }
        }

        internal fun runTests(root: File): List<TestResult> {
            val results = mutableListOf<TestResult>()
            root.walkTopDown().filter { it.name == "quicktest.kts" }.forEach { file ->
                val outputDir = Files.createTempDirectory("qtcompile").toFile()
                QuickTestUtils.compileQuickTest(file, outputDir)
                val className = file.nameWithoutExtension.replaceFirstChar { it.uppercase() } + "Kt"
                val loader = URLClassLoader(arrayOf(outputDir.toURI().toURL()), ClassLoader.getSystemClassLoader())
                val clazz = loader.loadClass(className)
                clazz.declaredMethods.filter { it.parameterCount == 0 }.forEach { method ->
                    try {
                        method.invoke(null)
                        results += TestResult(file, method.name, true, null)
                    } catch (t: Throwable) {
                        results += TestResult(file, method.name, false, t.cause ?: t)
                    }
                }
            }
            return results
        }
    }
}
