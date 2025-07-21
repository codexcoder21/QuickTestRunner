package community.kotlin.psi.leakproof.readonly

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument

fun printAnnotationName(entry: KtAnnotationEntry) {
    val name: Name? = entry.shortName
    println(name?.asString())
}

fun printArguments(entry: KtAnnotationEntry) {
    val arg = entry.valueArguments.singleOrNull() as? KtValueArgument
    val text = arg?.getStringTemplateExpression()?.text
    println(text)
}

