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
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.unscramble.UnscrambleSupport
import okio.Buffer
import proguard.retrace.ReTrace
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.LineNumberReader
import java.io.PrintWriter
import java.io.StringReader
import java.util.ResourceBundle
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ProguardRetraceUnscrambler : UnscrambleSupport<JComponent> {
    override fun getPresentableName() = "Proguard Retrace"

    override fun unscramble(project: Project, text: String, logName: String, settings: JComponent?): String? {
        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName).also {
            if (it.exists().not()) {
                ErrorDialog(project, it.name).show()
                return@unscramble null
            }
        }

        return LineNumberReader(StringReader(text)).use { reader ->
            val buffer = Buffer()
            PrintWriter(buffer.outputStream()).use { writer ->
                ReTrace(mappingFile).retrace(reader, writer)
                buffer.readUtf8()
            }
        }
    }
}

class ErrorDialog(project: Project, private val fileName: String) : DialogWrapper(project, false) {
    private val bundle = ResourceBundle.getBundle("Bundle")

    init {
        init()
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    override fun createCenterPanel(): JComponent? = JPanel(BorderLayout()).apply {
        JLabel(
            bundle.getString("error_text").format(fileName),
            Messages.getWarningIcon(),
            SwingConstants.HORIZONTAL
        ).apply {
            iconTextGap = 10
            preferredSize = Dimension(300, 100)
        }.let {
            add(it, BorderLayout.CENTER)
        }
    }
}
