/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ValueProperty
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

/**
 * Generates a file that defines a converter for an SDK defined value and its AIDL parcelable
 * counterpart.
 */
class ValueConverterFileGenerator(private val binderConverter: BinderCodeConverter) {

    fun generate(value: AnnotatedValue) =
        FileSpec.builder(
            value.converterNameSpec().packageName,
            value.converterNameSpec().simpleName
        ).build {
            addCommonSettings()
            addType(generateConverter(value))
        }

    private fun generateConverter(value: AnnotatedValue) =
        TypeSpec.objectBuilder(value.converterNameSpec()).build {
            addFunction(generateFromParcelable(value))
            addFunction(generateToParcelable(value))
        }

    private fun generateToParcelable(value: AnnotatedValue) =
        FunSpec.builder(value.toParcelableNameSpec().simpleName).build {
            addParameter("annotatedValue", value.type.poetSpec())
            returns(value.parcelableNameSpec())
            addStatement("val parcelable = %T()", value.parcelableNameSpec())
            value.properties.map(::generateToParcelablePropertyConversion).forEach(::addCode)
            addStatement("return parcelable")
        }

    private fun generateToParcelablePropertyConversion(property: ValueProperty) =
        CodeBlock.builder().build {
            addStatement(
                "parcelable.${property.name} = %L",
                binderConverter.convertToBinderCode(
                    property.type,
                    "annotatedValue.${property.name}"
                )
            )
        }

    private fun generateFromParcelable(value: AnnotatedValue) =
        FunSpec.builder(value.fromParcelableNameSpec().simpleName).build {
            addParameter("parcelable", value.parcelableNameSpec())
            returns(value.type.poetSpec())
            val parameters = value.properties.map(::generateFromParcelablePropertyConversion)
            addStatement {
                add("val annotatedValue = %T(\n", value.type.poetSpec())
                add(parameters.joinToCode(separator = ",\n"))
                add(")")
            }
            addStatement("return annotatedValue")
        }

    private fun generateFromParcelablePropertyConversion(property: ValueProperty) =
        CodeBlock.builder().build {
            add(
                "${property.name} = %L",
                binderConverter.convertToModelCode(
                    property.type, "parcelable.${property.name}"
                )
            )
        }
}