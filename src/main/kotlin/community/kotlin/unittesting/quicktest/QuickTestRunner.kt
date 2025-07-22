package community.kotlin.unittesting.quicktest

import community.kotlin.psi.leakproof.withKtFile
import kompile.Workspace
import kompiler.effects.DiagnosticEffect
import kompiler.effects.DiagnosticSeverity
import kotlinx.algebraiceffects.Effective
import kotlinx.algebraiceffects.NotificationEffect
import kotlinx.algebraiceffects.toss
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

import community.kotlin.unittesting.quicktest.TestStatus
import org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.system.exitProcess

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
        val results = Effective<List<TestResult>> {
            runTests(workspaceFs, workspace)
        }
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
                addOption(Option.builder().longOpt("verbose").desc("Print all test results").build())
            }
            val cmd = DefaultParser().parse(options, args)
            if (cmd.hasOption("help")) {
                org.apache.commons.cli.HelpFormatter().printHelp("QuickTestRunner", options)
                return
            }
            val logPath = cmd.getOptionValue("log")
            val workspacePath = cmd.getOptionValue("workspace", ".")
            val verbose = cmd.hasOption("verbose")
            val runner = QuickTestRunner().workspace(File(workspacePath))
            if (logPath != null) runner.logFile(File(logPath))
            val results = Effective<QuickTestRunResults> {
                handler { e: PackageWarningEffect ->
                    System.err.println(e.message)
                }
                handler { e: NotificationEffect ->
                    if (verbose || (e is DiagnosticEffect && (e.diagnostic.severity == DiagnosticSeverity.ERROR || e.diagnostic.severity == DiagnosticSeverity.WARNING))) {
                        e.printTinyTrace()
                    }
                }
                runner.run()
            }
            val outputResults = if (verbose) results.results else results.results.filter {
                it.status != TestStatus.SUCCESS && it.status != TestStatus.DISABLED
            }
            outputResults.forEach { result ->
                val qualifiedName = if (result.packageName.isNotEmpty()) "${result.packageName}.${result.function}" else result.function
                if (result.status == TestStatus.SUCCESS) {
                    println("PASSED $qualifiedName")
                } else if (result.status == TestStatus.DISABLED) {
                    println("DISABLED $qualifiedName")
                } else {
                    println("FAILED $qualifiedName -> ${result.error?.message}")
                    if (verbose) {
                        result.error?.printStackTrace()
                    }
                }
            }

            val successCount = results.results.count { it.status == TestStatus.SUCCESS }
            val failureCount = results.results.count { it.status == TestStatus.FAILURE }
            val disabledCount = results.results.count { it.status == TestStatus.DISABLED }
            val total = results.results.size - disabledCount
            if (failureCount == 0) {
                println("ALL TESTS PASSED (${successCount}/${total} tests completed successfully)")
            } else {
                println("TEST FAILURES: ${failureCount} failures.  ${successCount} success. (only ${successCount}/${total} tests completed successfully)")
            }
            if (disabledCount > 0) {
                println("NOTE: ${disabledCount} unit tests disabled")
            }
            exitProcess(if (failureCount == 0) 0 else 1)
        }

        internal fun runTests(workspaceFs: FileSystem, workspaceRoot: Path): List<TestResult> {
            val results = mutableListOf<TestResult>()
            workspaceRoot.toFile().walkTopDown().toList().filter { it.name == "test.kts" }.forEach { file ->
                val tempDir = Files.createTempDirectory("qtcompile")
                val outputDir = tempDir.toOkioPath()

                val ktPath = tempDir.resolve(file.name.substringBeforeLast('.') + ".kt")
                workspaceFs.source(file.toOkioPath()).use { src ->
                    FileSystem.SYSTEM.sink(ktPath.toOkioPath()).buffer().use { out ->
                        out.writeAll(src)
                    }
                }
                val pkgRegex = Regex("^\\s*package\\s+([\\w.]+)")
                val lines = workspaceFs.source(file.toOkioPath()).buffer().use { src ->
                    src.readUtf8().lineSequence().toList()
                }
                val pkgIndex = lines.indexOfFirst { line -> pkgRegex.containsMatchIn(line) }
                val packageName = if (pkgIndex != -1) pkgRegex.find(lines[pkgIndex])?.groupValues?.get(1) ?: "" else ""
                val firstCodeIndex = lines.indexOfFirst { line ->
                    val t = line.trim()
                    t.isNotEmpty() && !t.startsWith("@file:")
                }
                if (pkgIndex == -1 || pkgIndex != firstCodeIndex) {
                    toss(PackageWarningEffect(file.absolutePath))
                }
                val buildRules = getBuildRules(file)
                val repositories = getRepositories(file)
                    .ifEmpty { listOf("http://kotlin.directory/", "https://repo1.maven.org/maven2/") }

                try {
                    validateTestFile(file)

                    val wsRoot = workspaceFs.canonicalize(workspaceRoot).toFile()
                    val cpFiles = buildRules.flatMap { buildRule ->
                        runBuildRule(wsRoot, buildRule, repositories)
                    } + getBuildAnnotationsJar()

                QuickTestUtils.compileKotlin(listOf(ktPath.toFile()), cpFiles, outputDir.toNioPath().toFile())
                val baseClassName = file.name.substringBeforeLast('.').replaceFirstChar { it.uppercase() } + "Kt"
                val className = if (packageName.isNotEmpty()) "$packageName.$baseClassName" else baseClassName
                val loaderUrls = arrayOf(outputDir.toNioPath().toUri().toURL()) + cpFiles.map { it.toURI().toURL() }.toTypedArray()
                val loader = URLClassLoader(loaderUrls, ClassLoader.getSystemClassLoader())
                val clazz = loader.loadClass(className)
                val disabledAnnotation = Class.forName("build.kotlin.annotations.Disabled").asSubclass(Annotation::class.java)
                clazz.declaredMethods.filter { it.parameterCount == 0 }.forEach { method ->
                    if (method.getAnnotation(disabledAnnotation) != null) {
                        results += TestResult(file.toOkioPath(), method.name, packageName, TestStatus.DISABLED, null)
                    } else {
                        try {
                            method.invoke(null)
                            results += TestResult(file.toOkioPath(), method.name, packageName, TestStatus.SUCCESS, null)
                        } catch (t: Throwable) {
                            results += TestResult(file.toOkioPath(), method.name, packageName, TestStatus.FAILURE, t.cause ?: t)
                        }
                    }
                }
                } catch (t: Throwable) {
                    results += TestResult(file.toOkioPath(), "<failed to compile kts file: ${file.absolutePath}>", packageName, TestStatus.FAILURE, t)
                }
            }
            return results
        }
    }
}

// TODO: Should provide annotations from an embedded jar (instead of fetching from maven) in order to enable offline execution.
private fun getBuildAnnotationsJar(): List<File> {
    val withArtifact = Dependency.of(
        "build.kotlin.withartifact",
        "build-kotlin-withartifact",
        "0.0.1"
    )
    val annotations = Dependency.of(
        "build.kotlin.annotations",
        "build-kotlin-annotations",
        "0.0.1"
    )
    val fetch = Fetch.create()
    val coursierRepositories = listOf("http://kotlin.directory")
        .map { repoUrl -> MavenRepository.of(repoUrl) }
        .toTypedArray()
    fetch.withRepositories(*coursierRepositories)
    fetch.withDependencies(withArtifact.withTransitive(false))
    fetch.addDependencies(annotations.withTransitive(false))
    return fetch.fetch()
}

private fun getBuildRules(file: File): List<String> {
    return withKtFile(file) { ktFile ->
        ktFile.annotationEntries.filter{it.shortName!!.identifier == "build.kotlin.withartifact.WithArtifact" || it.shortName!!.identifier == "WithArtifact"}.map { annotationEntry ->
            val buildRule = annotationEntry.valueArgumentList!!.arguments.single().text.removeSurrounding("\"")
            buildRule
        }
    }
}

private fun getRepositories(file: File): List<String> {
    return withKtFile(file) { ktFile ->
        ktFile.annotationEntries.filter {
            it.shortName!!.identifier == "build.kotlin.withartifact.WithRepository" ||
                    it.shortName!!.identifier == "WithRepository"
        }.map { annotationEntry ->
            annotationEntry.valueArgumentList!!.arguments.single().text.removeSurrounding("\"")
        }
    }
}

private fun runBuildRule(workspaceDir: File, rule: String, repositories: List<String>): List<File> {
    val mavenPattern = Regex("^[^:]+:[^:]+:[^:]+$")
    if (mavenPattern.matches(rule)) {
        val parts = rule.split(":")
        val dep = Dependency.of(parts[0], parts[1], parts[2])
        val fetch = Fetch.create()
        val coursierRepositories = repositories.map { repoUrl -> MavenRepository.of(repoUrl) }.toTypedArray()
        fetch.withRepositories(*coursierRepositories)
        fetch.addDependencies(dep)
        try {
            return fetch.fetch()
        } catch (e: Exception) {
            val reposString = repositories.joinToString(", ")
            throw RuntimeException("Failed to fetch artifact $rule using maven repositories: $reposString", e)
        }
    }

    val workspace = Workspace(workspaceDir)
    val out = kotlin.io.path.createTempFile("qtbuild", null).toFile().apply { delete() }
    workspace.execute(rule, out)
    val resultFile = out
    if (resultFile.isDirectory) {
        val jar = resultFile.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".jar") }
            ?: throw IllegalStateException("No jar produced by build rule $rule")
        return listOf(jar)
    }
    return listOf(resultFile)
}

private fun validateTestFile(file: File) {
    withKtFile(file) { ktFile ->
        val topLevel = ktFile.declarations.flatMap { decl ->
            if (decl is org.jetbrains.kotlin.psi.KtScript) decl.declarations else listOf(decl)
        }
        val invalid = topLevel.filterNot { it is KtNamedFunction }
        if (invalid.isNotEmpty()) {
            val names = invalid.joinToString(", ") { it::class.simpleName ?: "unknown" }
            throw IllegalArgumentException("test.kts may only contain top-level functions. Found: $names")
        }

        val nonUnit = topLevel.filterIsInstance<KtNamedFunction>().filter {
            it.typeReference != null || !it.hasBlockBody()
        }
        if (nonUnit.isNotEmpty()) {
            val names = nonUnit.joinToString(", ") { it.name ?: "<anonymous>" }
            throw IllegalArgumentException(
                "test.kts functions must not declare return types and must use block bodies. Offending functions: $names"
            )
        }
    }
}
