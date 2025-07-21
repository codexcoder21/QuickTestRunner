package community.kotlin.test.quicktest

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.net.URLClassLoader
import java.net.URI
import java.nio.file.Files
import community.kotlin.psi.leakproof.withKtFile
import kompile.Workspace
import kompiler.effects.DiagnosticEffect
import kompiler.effects.DiagnosticSeverity
import kotlinx.algebraiceffects.Effective
import kotlinx.algebraiceffects.NotificationEffect
import kotlin.io.path.createTempFile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath

/** Entry point and core runner for quick tests. */
class QuickTestRunner {
    private var dirFs: FileSystem = FileSystem.SYSTEM
    private var directory: Path = ".".toPath()

    private var workspaceFs: FileSystem = FileSystem.SYSTEM
    private var workspaceRoot: Path = ".".toPath()

    private var logFs: FileSystem = FileSystem.SYSTEM
    private var logFile: Path? = null


    fun directory(fs: FileSystem, dir: Path): QuickTestRunner = apply {
        dirFs = fs
        directory = dir
    }

    fun logFile(fs: FileSystem, file: Path): QuickTestRunner = apply {
        logFs = fs
        logFile = file
    }

    fun workspace(fs: FileSystem, root: Path): QuickTestRunner = apply {
        workspaceFs = fs
        workspaceRoot = root
    }

    private fun configureProxyFromEnv() {
        val proxy = System.getenv("HTTP_PROXY") ?: return
        try {
            val uri = URI(proxy)
            val host = uri.host ?: return
            val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
            System.setProperty("http.proxyHost", host)
            System.setProperty("https.proxyHost", host)
            System.setProperty("http.proxyPort", port.toString())
            System.setProperty("https.proxyPort", port.toString())
        } catch (e: Exception) {
            System.err.println("Ignoring malformed HTTP_PROXY '$proxy': ${e.message}")
        }
    }


    fun run(): QuickTestRunResults {
        configureProxyFromEnv()
        val results = runTests(dirFs, directory, workspaceFs, workspaceRoot)
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
                addOption(Option.builder().longOpt("workspace").hasArg().desc("Workspace root path").build())
            }
            val cmd = DefaultParser().parse(options, args)
            if (cmd.hasOption("help")) {
                org.apache.commons.cli.HelpFormatter().printHelp("QuickTestRunner", options)
                return
            }
            val dirPath = cmd.getOptionValue("directory", ".")
            val logPath = cmd.getOptionValue("log")
            val wsPath = cmd.getOptionValue("workspace", dirPath)
            val runner = QuickTestRunner()
                .directory(File(dirPath))
                .workspace(File(wsPath))
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

        internal fun runTests(
            dirFs: FileSystem,
            root: Path,
            workspaceFs: FileSystem,
            workspaceRoot: Path
        ): List<TestResult> {
            val results = mutableListOf<TestResult>()
            dirFs.listRecursively(root).filter { it.name == "quicktest.kts" }.forEach { file ->
                val tempDir = Files.createTempDirectory("qtcompile")
                val outputDir = tempDir.toOkioPath()

                val buildRules = mutableListOf<String>()
                withKtFile(dirFs.canonicalize(file).toFile()) { ktFile ->
                    ktFile.annotationEntries.filter { it.shortName!!.identifier == "DependsOn" }.forEach { entry ->
                        val rule = entry.valueArgumentList!!.arguments.single().text.removeSurrounding("\"")
                        buildRules += rule
                    }
                }

                val cpFiles = buildRules.map {
                    val wsRoot = workspaceFs.canonicalize(workspaceRoot).toFile()
                    runBuildRule(wsRoot, it)
                }

                val runtimeCp = System.getProperty("java.class.path")
                    .split(File.pathSeparator)
                    .filter { it.isNotBlank() }
                    .map { File(it) }

                val classpathStr = (cpFiles.map { it.absolutePath } + runtimeCp.map { it.absolutePath }).joinToString(File.pathSeparator)

                val sourceFile = dirFs.canonicalize(file).toFile()
                val baseName = file.name.substringBeforeLast('.')
                val copy = File(outputDir.toFile(), "$baseName.kt")
                sourceFile.copyTo(copy, overwrite = true)
                QuickTestUtils.compileTests(
                    listOf(copy),
                    cpFiles + runtimeCp,
                    outputDir.toFile()
                )

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

        private fun runBuildRule(workspaceDir: File, rule: String): File {
            val workspace = Workspace(workspaceDir)
            val out = kotlin.io.path.createTempFile("qtbuild", null).toFile().apply { delete() }
            Effective {
                handler { e: NotificationEffect ->
                    if (e is DiagnosticEffect && (e.diagnostic.severity == DiagnosticSeverity.ERROR || e.diagnostic.severity == DiagnosticSeverity.WARNING)) {
                        e.printTinyTrace()
                    }
                }
                workspace.execute(rule, out)
            }
            val resultFile = out
            if (resultFile.isDirectory) {
                val jar = resultFile.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".jar") }
                    ?: throw IllegalStateException("No jar produced by build rule $rule")
                return jar
            }
            return resultFile
        }
    }
}
