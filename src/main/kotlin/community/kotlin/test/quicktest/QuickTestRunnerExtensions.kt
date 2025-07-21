package community.kotlin.test.quicktest

import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/** Convenience extension functions using java.io.File APIs. */
fun QuickTestRunner.directory(dir: File): QuickTestRunner =
    directory(FileSystem.SYSTEM, dir.toPath().toOkioPath())

fun QuickTestRunner.logFile(file: File): QuickTestRunner =
    logFile(FileSystem.SYSTEM, file.toPath().toOkioPath())

fun QuickTestRunner.workspace(dir: File): QuickTestRunner =
    workspace(FileSystem.SYSTEM, dir.toPath().toOkioPath())
