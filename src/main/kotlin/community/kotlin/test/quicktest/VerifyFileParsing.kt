package community.kotlin.test.quicktest


import community.kotlin.psi.annotationutils.readFileAnnotationEntries
import community.kotlin.psi.leakproof.withKtFile
import java.io.File

fun main() {
    val file = File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests/quicktest.kts").absoluteFile
  /*  val annotations = readFileAnnotationEntries(file)
    for(annotation in annotations)
        print(annotation.getValueArguments().single().getStringTemplateExpression()!!.text)
*/

    val buildRules = mutableListOf<String>()
    withKtFile(file) { ktFile ->
        ktFile.annotationEntries.filter{it.shortName!!.identifier == "DependsOn"}.forEach { annotationEntry ->
            val buildRule = annotationEntry.valueArgumentList!!.arguments.single().text.removeSurrounding("\"")
            buildRules.add(buildRule)
        }
    }

    println(buildRules)
}


