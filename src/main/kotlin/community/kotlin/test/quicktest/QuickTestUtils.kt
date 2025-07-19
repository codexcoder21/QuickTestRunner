package community.kotlin.test.quicktest

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.apache.commons.text.StringEscapeUtils
import java.io.File

/** Utility functions for compiling and reporting quick tests. */
object QuickTestUtils {
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

    fun writeResults(results: List<TestResult>, logFile: File) {
        when {
            logFile.name.endsWith(".xml") -> writeXml(results, logFile)
            logFile.name.endsWith(".html") -> writeHtml(results, logFile)
            else -> writePlain(results, logFile)
        }
    }

    private fun writeXml(results: List<TestResult>, file: File) {
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

    private fun writeHtml(results: List<TestResult>, file: File) {
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

    private fun writePlain(results: List<TestResult>, file: File) {
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

    private fun String.xmlEscape(): String =
        StringEscapeUtils.escapeXml11(this)

    private fun String.htmlEscape(): String =
        StringEscapeUtils.escapeHtml4(this)
}
