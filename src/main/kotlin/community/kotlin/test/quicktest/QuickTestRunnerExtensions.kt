package community.kotlin.test.quicktest

import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/** Convenience extension functions using java.io.File APIs. */
fun QuickTestRunner.directory(dir: File): QuickTestRunner =
    directory(FileSystem.SYSTEM, dir.toPath().toOkioPath())

fun QuickTestRunner.logFile(file: File): QuickTestRunner =
    logFile(FileSystem.SYSTEM, file.toPath().toOkioPath())

fun QuickTestRunner.classpath(vararg files: File): QuickTestRunner =
    classpath(FileSystem.SYSTEM, *files.map { it.toPath().toOkioPath() }.toTypedArray())

fun QuickTestRunner.classpath(cp: String): QuickTestRunner {
    val paths = cp.split(File.pathSeparator)
        .filter { it.isNotBlank() }
        .map { File(it).toPath().toOkioPath() }
    return classpath(FileSystem.SYSTEM, *paths.toTypedArray())
}
