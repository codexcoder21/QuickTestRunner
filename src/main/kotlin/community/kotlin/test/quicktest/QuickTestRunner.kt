package community.kotlin.test.quicktest

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath

/** Entry point and core runner for quick tests. */
class QuickTestRunner {
    private var dirFs: FileSystem = FileSystem.SYSTEM
    private var directory: Path = ".".toPath()

    private var logFs: FileSystem = FileSystem.SYSTEM
    private var logFile: Path? = null

    private var cpFs: FileSystem = FileSystem.SYSTEM
    private var classpath: List<Path>? = null

    fun directory(fs: FileSystem, dir: Path): QuickTestRunner = apply {
        dirFs = fs
        directory = dir
    }

    fun logFile(fs: FileSystem, file: Path): QuickTestRunner = apply {
        logFs = fs
        logFile = file
    }

    fun classpath(fs: FileSystem, vararg cp: Path): QuickTestRunner = apply {
        cpFs = fs
        classpath = cp.toList()
    }

    fun run(): QuickTestRunResults {
        val results = runTests(dirFs, directory, cpFs, classpath)
        val log = logFile
        if (log != null) QuickTestUtils.writeResults(results, logFs, log)
        return QuickTestRunResults(results)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val options = Options().apply {
                addOption(Option.builder("h")
                    .longOpt("help")
                    .desc("Print this help message")
                    .build())
                addOption(Option.builder().longOpt("directory").hasArg().desc("Directory to scan").build())
                addOption(Option.builder().longOpt("log").hasArg().desc("Log file to dump results").build())
                addOption(Option.builder().longOpt("classpath").hasArg().desc("Extra classpath for compiling and running tests").build())
            }
            val cmd = DefaultParser().parse(options, args)
            if (cmd.hasOption("help")) {
                org.apache.commons.cli.HelpFormatter().printHelp("QuickTestRunner", options)
                return
            }
            val dirPath = cmd.getOptionValue("directory", ".")
            val logPath = cmd.getOptionValue("log")
            val cp = cmd.getOptionValue("classpath")
            val runner = QuickTestRunner().directory(File(dirPath))
            if (logPath != null) runner.logFile(File(logPath))
            if (cp != null) runner.classpath(cp)
            val results = runner.run()
            results.results.forEach { result ->
                if (result.success) {
                    println("PASSED ${result.file}:${result.function}")
                } else {
                    println("FAILED ${result.file}:${result.function} -> ${result.error?.message}")
                }
            }
        }

        internal fun runTests(dirFs: FileSystem, root: Path, cpFs: FileSystem, cp: List<Path>? = null): List<TestResult> {
            val results = mutableListOf<TestResult>()
            dirFs.listRecursively(root).filter { it.name == "quicktest.kts" }.forEach { file ->
                val tempDir = Files.createTempDirectory("qtcompile")
                val outputDir = tempDir.toOkioPath()
                val classpathStr = cp?.joinToString(File.pathSeparator) { cpFs.canonicalize(it).toString() }
                    ?: System.getProperty("java.class.path")
                // TODO: Do compile of the quicktest.kts file using classpath (see `compileKotlin` function).  Make sure quicktest.kts gets a name ending in `.kt` before compiling, to avoid bugs with kts files.
                val className = file.name.substringBeforeLast('.').replaceFirstChar { it.uppercase() } + "Kt"
                val cpUrls = classpathStr.split(File.pathSeparator)
                    .filter { it.isNotBlank() }
                    .map { File(it).toURI().toURL() }
                val loaderUrls = arrayOf(outputDir.toNioPath().toUri().toURL()) + cpUrls.toTypedArray()
                val loader = URLClassLoader(loaderUrls, ClassLoader.getSystemClassLoader())
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
