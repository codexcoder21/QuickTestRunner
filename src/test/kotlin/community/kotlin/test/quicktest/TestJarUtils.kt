package community.kotlin.test.quicktest

import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.StandardLocation
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler

fun createMathJar(): File {
    val tmpDir = kotlin.io.path.createTempDirectory("math").toFile()
    val srcFile = File(tmpDir, "MathOps.java")
    srcFile.writeText(
        """
        public class MathOps {
            public static int add(int a,int b){ return a+b; }
            public static int subtract(int a,int b){ return a-b; }
            public static int multiply(int a,int b){ return a*b; }
            public static int divide(int a,int b){ return a/b; }
        }
        """.trimIndent()
    )
    val classesDir = File(tmpDir, "classes").apply { mkdirs() }
    val compiler = EclipseCompiler()
    val fileManager = compiler.getStandardFileManager(null, null, null)
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(classesDir))
    val units = fileManager.getJavaFileObjects(srcFile)
    val task = compiler.getTask(null, fileManager, null, null, null, units)
    if (!task.call()) throw RuntimeException("Compilation failed")
    fileManager.close()
    val jarFile = File(tmpDir, "math.jar")
    JarOutputStream(FileOutputStream(jarFile)).use { jarOut ->
        classesDir.walkTopDown().filter { it.isFile }.forEach { f ->
            val entryName = f.relativeTo(classesDir).path.replace(File.separatorChar, '/')
            jarOut.putNextEntry(JarEntry(entryName))
            f.inputStream().use { it.copyTo(jarOut) }
            jarOut.closeEntry()
        }
    }
    return jarFile
}
