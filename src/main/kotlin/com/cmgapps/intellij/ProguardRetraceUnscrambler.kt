/*
 * Copyright (c) 2020. Christian Grach <christian.grach@cmgapps.com>
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

package com.cmgapps.intellij

import com.intellij.openapi.project.Project
import com.intellij.unscramble.UnscrambleSupport
import okio.Buffer
import proguard.retrace.ReTrace
import java.io.File
import java.io.LineNumberReader
import java.io.PrintWriter
import java.io.StringReader
import javax.swing.JComponent

class ProguardRetraceUnscrambler : UnscrambleSupport<JComponent> {
    override fun getPresentableName() = "Proguard Retrace"

    override fun unscramble(project: Project, text: String, logName: String, settings: JComponent?): String? {
        val mappingFile = File(logName)
        if (logName.isBlank() || mappingFile.exists().not()) {
            return null
        }

        if (text.isBlank()) {
            return ""
        }

        return LineNumberReader(StringReader(text)).use { reader ->
            val buffer = Buffer()
            PrintWriter(buffer.outputStream()).use { writer ->
                ReTrace(ReTrace.STACK_TRACE_EXPRESSION, false, mappingFile).retrace(reader, writer)
                buffer.readUtf8()
            }
        }
    }
}