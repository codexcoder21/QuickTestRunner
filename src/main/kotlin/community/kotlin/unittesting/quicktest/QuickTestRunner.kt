package community.kotlin.unittesting.quicktest

import community.kotlin.psi.leakproof.withKtFile
import kompile.Workspace
import kompiler.effects.DiagnosticEffect
import kompiler.effects.DiagnosticSeverity
import kotlinx.algebraiceffects.Effective
import kotlinx.algebraiceffects.NotificationEffect
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import coursierapi.Dependency
import coursierapi.Fetch
import coursierapi.MavenRepository
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.buffer

/** Entry point and core runner for quick tests. */
class QuickTestRunner {

    private var logFs: FileSystem = FileSystem.SYSTEM
    private var logFile: Path? = null

    private var workspaceFs: FileSystem = FileSystem.SYSTEM
    private var workspace: Path = ".".toPath()


    fun logFile(fs: FileSystem, file: Path): QuickTestRunner = apply {
        logFs = fs
        logFile = file
    }

    fun workspace(fs: FileSystem, dir: Path): QuickTestRunner = apply {
        workspaceFs = fs
        workspace = dir
    }

    fun run(): QuickTestRunResults {
        val results = runTests(workspaceFs, workspace)
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
                addOption(Option.builder().longOpt("log").hasArg().desc("Log file to dump results").build())
                addOption(Option.builder().longOpt("workspace").hasArg().desc("Workspace root directory").build())
            }
            val cmd = DefaultParser().parse(options, args)
            if (cmd.hasOption("help")) {
                org.apache.commons.cli.HelpFormatter().printHelp("QuickTestRunner", options)
                return
            }
            val logPath = cmd.getOptionValue("log")
            val workspacePath = cmd.getOptionValue("workspace", ".")
            val runner = QuickTestRunner().workspace(File(workspacePath))
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

        internal fun runTests(workspaceFs: FileSystem, workspaceRoot: Path): List<TestResult> {
            val results = mutableListOf<TestResult>()
            workspaceRoot.toFile().walkTopDown().toList().filter { it.name == "quicktest.kts" }.forEach { file ->
                val tempDir = Files.createTempDirectory("qtcompile")
                val outputDir = tempDir.toOkioPath()

                val ktPath = tempDir.resolve(file.name.substringBeforeLast('.') + ".kt")
                workspaceFs.source(file.toOkioPath()).use { src ->
                    FileSystem.SYSTEM.sink(ktPath.toOkioPath()).buffer().use { out ->
                        out.writeAll(src)
                    }
                }
                val buildRules = getBuildRules(file)

                val cpFiles = buildRules.flatMap { buildRule ->
                    val wsRoot = workspaceFs.canonicalize(workspaceRoot).toFile()
                    runBuildRule(wsRoot, buildRule)
                } + System.getProperty("java.class.path")  // TODO: Should not be passing in system/parent classpath once we can import artifacts via maven.
                    .split(File.pathSeparator)
                    .filter { it.isNotBlank() }
                    .map { File(it) }
                    .filter { !it.absolutePath.contains("QuickTest") }
                    .filter { !it.absolutePath.contains("2.2.0") }

                cpFiles.forEach {
                    println(it.absolutePath)
                }

                QuickTestUtils.compileKotlin(listOf(ktPath.toFile()), cpFiles, outputDir.toNioPath().toFile())
                val className = file.name.substringBeforeLast('.').replaceFirstChar { it.uppercase() } + "Kt"
                val loaderUrls = arrayOf(outputDir.toNioPath().toUri().toURL()) + cpFiles.map { it.toURI().toURL() }.toTypedArray()
                val loader = URLClassLoader(loaderUrls, ClassLoader.getSystemClassLoader())
                val clazz = loader.loadClass(className)
                clazz.declaredMethods.filter { it.parameterCount == 0 }.forEach { method ->
                    try {
                        method.invoke(null)
                        results += TestResult(file.toOkioPath(), method.name, true, null)
                    } catch (t: Throwable) {
                        results += TestResult(file.toOkioPath(), method.name, false, t.cause ?: t)
                    }
                }
            }
            return results
        }
    }
}


private fun getBuildRules(file: File): List<String> {
    return withKtFile(file) { ktFile ->
        ktFile.annotationEntries.filter{it.shortName!!.identifier == "build.kotlin.withartifact.WithArtifact" || it.shortName!!.identifier == "WithArtifact"}.map { annotationEntry ->
            val buildRule = annotationEntry.valueArgumentList!!.arguments.single().text.removeSurrounding("\"")
            buildRule
        }
    }
}

private fun runBuildRule(workspaceDir: File, rule: String): List<File> {
    val mavenPattern = Regex("^[^:]+:[^:]+:[^:]+$")
    if (mavenPattern.matches(rule)) {
        val parts = rule.split(":")
        val dep = Dependency.of(parts[0], parts[1], parts[2])
        return Fetch.create()
            .addRepositories(MavenRepository.of("https://repo1.maven.org/maven2/"))
            .addDependencies(dep)
            .fetch()
    }

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
        return listOf(jar)
    }
    return listOf(resultFile)
}
