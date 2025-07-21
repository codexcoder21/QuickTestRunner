package community.kotlin.test.quicktest

import community.kotlin.psi.annotationutils.readFileAnnotationEntries
import community.kotlin.psi.leakproof.readonly.KtAnnotationEntry
import java.io.File

/** Utility program that prints the number of file-level annotations. */
object PrintAnnotations {
    @JvmStatic
    fun main(args: Array<String>) {
        val file = if (args.isNotEmpty()) File(args[0]) else File("Example.kts")
        val annotations: List<KtAnnotationEntry> = readFileAnnotationEntries(file)
        println(annotations.size)
    }
}
