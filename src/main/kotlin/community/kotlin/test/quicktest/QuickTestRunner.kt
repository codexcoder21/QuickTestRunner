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
    private var classpath: String? = null

    fun directory(dir: File): QuickTestRunner = apply { this.directory = dir }
    fun logFile(file: File): QuickTestRunner = apply { this.logFile = file }
    fun classpath(cp: String): QuickTestRunner = apply { this.classpath = cp }

    fun run(): QuickTestRunResults {
        val results = runTests(directory, classpath)
        logFile?.let { QuickTestUtils.writeResults(results, it) }
        return QuickTestRunResults(results)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options().apply {
                addOption(Option.builder().longOpt("directory").hasArg().desc("Directory to scan").build())
                addOption(Option.builder().longOpt("log").hasArg().desc("Log file to dump results").build())
                addOption(Option.builder().longOpt("classpath").hasArg().desc("Extra classpath for compiling and running tests").build())
            }
            val cmd = DefaultParser().parse(options, args)
            val dirPath = cmd.getOptionValue("directory", ".")
            val logPath = cmd.getOptionValue("log")
            val cpArg = cmd.getOptionValue("classpath")
            val runner = QuickTestRunner().directory(File(dirPath))
            if (logPath != null) runner.logFile(File(logPath))
            if (cpArg != null) runner.classpath(cpArg)
            val results = runner.run()
            results.results.forEach { result ->
                if (result.success) {
                    println("PASSED ${result.file}:${result.function}")
                } else {
                    println("FAILED ${result.file}:${result.function} -> ${result.error?.message}")
                }
            }
        }

        internal fun runTests(root: File, classpath: String? = null): List<TestResult> {
            val results = mutableListOf<TestResult>()
            root.walkTopDown().filter { it.name == "quicktest.kts" }.forEach { file ->
                val outputDir = Files.createTempDirectory("qtcompile").toFile()
                QuickTestUtils.compileQuickTest(file, outputDir, classpath)
                val className = file.nameWithoutExtension.replaceFirstChar { it.uppercase() } + "Kt"
                val urls = mutableListOf(outputDir.toURI().toURL())
                if (!classpath.isNullOrBlank()) {
                    classpath.split(File.pathSeparator).filter { it.isNotBlank() }.forEach {
                        urls += File(it).toURI().toURL()
                    }
                }
                val loader = URLClassLoader(urls.toTypedArray(), ClassLoader.getSystemClassLoader())
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
