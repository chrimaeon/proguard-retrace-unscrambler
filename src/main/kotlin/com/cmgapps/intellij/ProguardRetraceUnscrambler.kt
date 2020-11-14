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
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ProguardRetraceUnscrambler : UnscrambleSupport<JPanel> {
    private val bundle = ResourceBundle.getBundle("Bundle")
    override fun getPresentableName() = "Proguard Retrace"

    override fun unscramble(project: Project, text: String, logName: String, settings: JPanel?): String? {

        val allClassNamesSetting: Boolean
        val verboseSetting: Boolean

        if (settings == null) {
            allClassNamesSetting = false
            verboseSetting = false
        } else {
            allClassNamesSetting = (settings.getComponent(ALL_CLASS_NAMES_INDEX) as JCheckBox).isSelected
            verboseSetting = (settings.getComponent(VERBOSE_INDEX) as JCheckBox).isSelected
        }

        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName).also {
            if (it.exists().not()) {
                ErrorDialog(project,bundle, it.name).show()
                return@unscramble null
            }
        }

        return LineNumberReader(StringReader(text)).use { reader ->
            val buffer = Buffer()
            PrintWriter(buffer.outputStream()).use { writer ->
                ReTrace(ReTrace.REGULAR_EXPRESSION, allClassNamesSetting, verboseSetting, mappingFile).retrace(
                    reader,
                    writer
                )
                buffer.readUtf8()
            }
        }
    }

    override fun createSettingsComponent(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JCheckBox(bundle.getString("all_class_names_text")), ALL_CLASS_NAMES_INDEX)
            add(JCheckBox(bundle.getString("verbose_text")), VERBOSE_INDEX)
        }
    }

    private companion object {
        private const val ALL_CLASS_NAMES_INDEX = 0
        private const val VERBOSE_INDEX = 1
    }
}

class ErrorDialog(project: Project, private val bundle: ResourceBundle, private val fileName: String) : DialogWrapper(project, false) {
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
