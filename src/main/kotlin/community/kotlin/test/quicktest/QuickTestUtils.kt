package community.kotlin.test.quicktest

import kompiler.effects.Diagnostic
import kompiler.effects.DiagnosticEffect
import kompiler.effects.DiagnosticSeverity
import kotlinc.diagnostic.collector.DiagnosticCollector
import kotlinx.algebraiceffects.toss
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.apache.commons.text.StringEscapeUtils
import okio.FileSystem
import okio.Path
import okio.buffer
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.config.Services
import java.io.File

/** Utility functions for compiling and reporting quick tests. */
object QuickTestUtils {

    fun compileKotlin(ktFiles: List<File>, classpath: List<File>, destination: File) {
        val compiler = K2JVMCompiler()
        val errStream = System.err
        val messageRenderer = MessageRenderer.PLAIN_RELATIVE_PATHS
        val arguments = compiler.createArguments()
        arguments.noStdlib = true
        arguments.useIR = true
        arguments.includeRuntime = false
        arguments.classpath = classpath.map { it.absolutePath }.joinToString(":")
        arguments.jvmTarget = "1.8"
        arguments.destination = destination.absolutePath
        arguments.freeArgs = ktFiles.map { it.absolutePath }
// TODO:            arguments.pluginClasspaths = pluginClasspaths.toTypedArray()
        try {
            errStream.print(messageRenderer.renderPreamble())
            val errorMessage = validateArguments(arguments.errors)
            if (errorMessage != null) {
                toss(DiagnosticEffect(Diagnostic(DiagnosticSeverity.ERROR, errorMessage, null)))
                toss(DiagnosticEffect(Diagnostic(DiagnosticSeverity.INFO, "Use -help for more information", null)))
                throw Error("compile failed!")
            }
            val collector = DiagnosticCollector(messageRenderer)
            val code = compiler.exec(collector, Services.EMPTY, arguments)
            collector.getDiagnostics().forEach {
                toss(DiagnosticEffect(it))
            }
            if (code != ExitCode.OK) throw Error("Compile failure: Non-zero exit code: $code")
        } finally {
            errStream.print(messageRenderer.renderConclusion())
        }
    }


    fun writeResults(results: List<TestResult>, fs: FileSystem, logFile: Path) {
        when {
            logFile.name.endsWith(".xml") -> writeXml(results, fs, logFile)
            logFile.name.endsWith(".html") -> writeHtml(results, fs, logFile)
            else -> writePlain(results, fs, logFile)
        }
    }

    private fun writeXml(results: List<TestResult>, fs: FileSystem, file: Path) {
        fs.sink(file).buffer().use { out ->
            out.writeUtf8("<tests>\n")
            results.forEach { r ->
                if (r.success) {
                    out.writeUtf8("  <test file=\"${r.file}\" name=\"${r.function}\" success=\"true\"/>\n")
                } else {
                    val stack = r.error?.stackTraceToString()?.xmlEscape()
                    out.writeUtf8("  <test file=\"${r.file}\" name=\"${r.function}\" success=\"false\">\n")
                    out.writeUtf8("    <stacktrace>${stack}</stacktrace>\n")
                    out.writeUtf8("  </test>\n")
                }
            }
            out.writeUtf8("</tests>\n")
        }
    }

    private fun writeHtml(results: List<TestResult>, fs: FileSystem, file: Path) {
        fs.sink(file).buffer().use { out ->
            out.writeUtf8("<html><body><table>\n")
            out.writeUtf8("<tr><th>File</th><th>Function</th><th>Status</th><th>Stacktrace</th></tr>\n")
            results.forEach { r ->
                val stack = if (r.success) "" else r.error?.stackTraceToString()?.htmlEscape()
                out.writeUtf8("<tr><td>${r.file}</td><td>${r.function}</td><td>${if (r.success) "PASSED" else "FAILED"}</td><td><pre>${stack}</pre></td></tr>\n")
            }
            out.writeUtf8("</table></body></html>\n")
        }
    }

    private fun writePlain(results: List<TestResult>, fs: FileSystem, file: Path) {
        fs.sink(file).buffer().use { out ->
            results.forEach { r ->
                if (r.success) {
                    out.writeUtf8("PASSED ${r.file}:${r.function}\n")
                } else {
                    out.writeUtf8("FAILED ${r.file}:${r.function}\n")
                    r.error?.stackTraceToString()?.let { out.writeUtf8(it + "\n") }
                }
            }
        }
    }

    private fun String.xmlEscape(): String =
        StringEscapeUtils.escapeXml11(this)

    private fun String.htmlEscape(): String =
        StringEscapeUtils.escapeHtml4(this)
}
