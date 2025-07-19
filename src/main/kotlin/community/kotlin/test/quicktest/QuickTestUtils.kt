package community.kotlin.test.quicktest

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.apache.commons.text.StringEscapeUtils
import okio.FileSystem
import okio.Path
import okio.buffer

/** Utility functions for compiling and reporting quick tests. */
object QuickTestUtils {
    fun compileQuickTest(srcFs: FileSystem, src: Path, destFs: FileSystem, outputDir: Path, classpath: String = System.getProperty("java.class.path")) {
        val srcName = src.name.substringBeforeLast('.')
        val tempKt = outputDir / (srcName + ".kt")
        destFs.delete(tempKt, mustExist = false)
        // Copy using java.io for portability
        val srcFile = srcFs.canonicalize(src).toNioPath().toFile()
        val outputDirFile = destFs.canonicalize(outputDir).toNioPath().toFile()
        outputDirFile.mkdirs()
        val tempKtFile = outputDirFile.resolve(srcName + ".kt")
        srcFile.copyTo(tempKtFile, overwrite = true)
        val args = arrayOf(
            "-classpath", classpath,
            "-d", outputDirFile.absolutePath,
            tempKtFile.absolutePath
        )
        val exit = CLICompiler.doMainNoExit(K2JVMCompiler(), args)
        if (exit != ExitCode.OK) {
            throw RuntimeException("Compilation failed for ${srcFs.canonicalize(src)}")
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
