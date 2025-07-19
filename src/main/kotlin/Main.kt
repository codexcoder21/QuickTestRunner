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
    }
    val cmd = DefaultParser().parse(options, args)
    val dirPath = cmd.getOptionValue("directory", ".")
    val results = runTests(File(dirPath))
    results.forEach { result ->
        if (result.success) {
            println("PASSED ${result.file}:${result.function}")
        } else {
            println("FAILED ${result.file}:${result.function} -> ${result.error?.message}")
        }
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

fun compileQuickTest(src: File, outputDir: File) {
    val tempKt = File(outputDir, src.nameWithoutExtension + ".kt")
    src.copyTo(tempKt, overwrite = true)
    val cp = System.getProperty("java.class.path")
    val args = arrayOf("-classpath", cp, "-d", outputDir.absolutePath, tempKt.absolutePath)
    val exit = CLICompiler.doMainNoExit(K2JVMCompiler(), args)
    if (exit != ExitCode.OK) {
        throw RuntimeException("Compilation failed for ${'$'}{src.absolutePath}")
    }
}
