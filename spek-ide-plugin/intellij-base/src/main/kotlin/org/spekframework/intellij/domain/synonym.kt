package org.spekframework.intellij.domain

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

enum class PsiSynonymType {
    GROUP,
    TEST
}

data class PsiSynonym(val annotation: PsiAnnotation) {
    val type: PsiSynonymType by lazy {
        val type = annotation.findAttributeValue("type")!!.text

        when {
            type.endsWith("SynonymType.GROUP") -> PsiSynonymType.GROUP
            type.endsWith("SynonymType.TEST") -> PsiSynonymType.TEST
            else -> throw IllegalArgumentException("Unsupported synonym: $type.")
        }
    }

    val prefix: String by lazy {
        annotation.findAttributeValue("prefix")!!.text.removeSurrounding("\"")
    }

    val excluded: Boolean by lazy {
        annotation.findAttributeValue("excluded")!!.text.toBoolean()
    }

    val runnable: Boolean by lazy {
        // default matches default from the runtime, this is needed
        // so the plugin will still work with old versions of spek2 that
        // does not have the runnable flag.
        annotation.findAttributeValue("runnable")?.let {
            it.text.toBoolean()
        } ?: true
    }
}

enum class PsiDescriptionLocation {
    TYPE_PARAMETER,
    VALUE_PARAMETER
}

class PsiDescription(val annotation: PsiAnnotation) {
    val index: Int by lazy {
        annotation.findAttributeValue("index")!!.text.toInt()
    }

    val location: PsiDescriptionLocation by lazy {
        val text = annotation.findAttributeValue("location")!!.text
        when {
            text.endsWith("DescriptionLocation.TYPE_PARAMETER") -> PsiDescriptionLocation.TYPE_PARAMETER
            text.endsWith("DescriptionLocation.VALUE_PARAMETER") -> PsiDescriptionLocation.VALUE_PARAMETER
            else -> throw IllegalArgumentException("Unknown location type: $text")
        }
    }
}

class PsiDescriptions(val annotation: PsiAnnotation) {
    val sources: Array<PsiDescription> by lazy {
        val value = annotation.findAttributeValue("sources")!!

        if (value is PsiArrayInitializerMemberValue) {
            value.initializers.map { it as PsiAnnotation }
                .map(::PsiDescription)
                .toTypedArray()
        } else if (value is KtLightAnnotationForSourceEntry) {
            arrayOf(PsiDescription(value))
        } else {
            throw AssertionError("Can not defer sources from $value.")
        }
    }
}

class UnsupportedFeatureException(msg: String): Throwable(msg)

class SynonymContext(val synonym: PsiSynonym, val descriptions: PsiDescriptions) {
    fun isExcluded() = synonym.excluded
    fun isRunnable() = synonym.runnable

    fun constructDescription(callExpression: KtCallExpression): String {
        return descriptions.sources.map {
            when (it.location) {
                PsiDescriptionLocation.TYPE_PARAMETER -> throw UnsupportedFeatureException("Type parameter description is currently unsupported.")
                PsiDescriptionLocation.VALUE_PARAMETER -> {
                    val argument = callExpression.valueArguments.getOrNull(it.index)
                    val expression = argument?.getArgumentExpression()

                    when (expression) {
                        is KtStringTemplateExpression -> {
                            if (!expression.hasInterpolation()) {
                                // might be empty at some point, especially when user is still typing
                                expression.entries.firstOrNull()?.text ?: ""
                            } else {
                                throw UnsupportedFeatureException("Descriptions with interpolation are currently unsupported.")
                            }
                        }
                        // can happen when user is still typing
                        else -> throw UnsupportedFeatureException("Value argument description should be a string.")
                    }
                }
                else -> throw IllegalArgumentException("Invalid location: ${it.location}")
            }
        }.fold(synonym.prefix) { prev, current ->
            if (prev.isNotEmpty()) {
                "$prev$current"
            } else {
                current
            }
        }
    }
}
