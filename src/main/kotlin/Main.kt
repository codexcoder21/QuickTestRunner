package org.example

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

data class TestResult(val file: File, val function: String, val success: Boolean, val error: Throwable?)

fun main(args: Array<String>) {
    val options = Options().apply {
        addOption(Option.builder().longOpt("directory").hasArg().desc("Directory to scan").build())
        addOption(Option.builder().longOpt("log").hasArg().desc("Log file to dump results").build())
    }
    val cmd = DefaultParser().parse(options, args)
    val dirPath = cmd.getOptionValue("directory", ".")
    val logPath = cmd.getOptionValue("log")
    val results = runTests(File(dirPath))
    results.forEach { result ->
        if (result.success) {
            println("PASSED ${result.file}:${result.function}")
        } else {
            println("FAILED ${result.file}:${result.function} -> ${result.error?.message}")
        }
    }
    if (logPath != null) {
        writeResults(results, File(logPath))
    }
}

fun runTests(root: File): List<TestResult> {
    val results = mutableListOf<TestResult>()
    root.walkTopDown().filter { it.name == "quicktest.kts" }.forEach { file ->
        val outputDir = Files.createTempDirectory("qtcompile").toFile()
        compileQuickTest(file, outputDir)
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

fun writeResults(results: List<TestResult>, logFile: File) {
    when {
        logFile.name.endsWith(".xml") -> writeXml(results, logFile)
        logFile.name.endsWith(".html") -> writeHtml(results, logFile)
        else -> writePlain(results, logFile)
    }
}

fun writeXml(results: List<TestResult>, file: File) {
    file.printWriter().use { out ->
        out.println("<tests>")
        results.forEach { r ->
            if (r.success) {
                out.println("  <test file=\"${r.file}\" name=\"${r.function}\" success=\"true\"/>")
            } else {
                val stack = r.error?.stackTraceToString()?.xmlEscape()
                out.println("  <test file=\"${r.file}\" name=\"${r.function}\" success=\"false\">")
                out.println("    <stacktrace>${stack}</stacktrace>")
                out.println("  </test>")
            }
        }
        out.println("</tests>")
    }
}

fun writeHtml(results: List<TestResult>, file: File) {
    file.printWriter().use { out ->
        out.println("<html><body><table>")
        out.println("<tr><th>File</th><th>Function</th><th>Status</th><th>Stacktrace</th></tr>")
        results.forEach { r ->
            val stack = if (r.success) "" else r.error?.stackTraceToString()?.htmlEscape()
            out.println("<tr><td>${r.file}</td><td>${r.function}</td><td>${if (r.success) "PASSED" else "FAILED"}</td><td><pre>${stack}</pre></td></tr>")
        }
        out.println("</table></body></html>")
    }
}

fun writePlain(results: List<TestResult>, file: File) {
    file.printWriter().use { out ->
        results.forEach { r ->
            if (r.success) {
                out.println("PASSED ${r.file}:${r.function}")
            } else {
                out.println("FAILED ${r.file}:${r.function}")
                r.error?.stackTraceToString()?.let { out.println(it) }
            }
        }
    }
}

fun String.xmlEscape(): String =
    this.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

fun String.htmlEscape(): String =
    this.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

fun compileQuickTest(src: File, outputDir: File) {
    val tempKt = File(outputDir, src.nameWithoutExtension + ".kt")
    src.copyTo(tempKt, overwrite = true)
    val cp = System.getProperty("java.class.path")
    val args = arrayOf("-classpath", cp, "-d", outputDir.absolutePath, tempKt.absolutePath)
    val exit = CLICompiler.doMainNoExit(K2JVMCompiler(), args)
    if (exit != ExitCode.OK) {
        throw RuntimeException("Compilation failed for ${src.absolutePath}")
    }
}
