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

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.unscramble.UnscrambleSupport
import okio.Buffer
import org.jetbrains.annotations.VisibleForTesting
import proguard.retrace.ReTrace
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.io.File
import java.io.LineNumberReader
import java.io.PrintWriter
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
    private val properties = PropertiesComponent.getInstance()

    override fun getPresentableName() = "Proguard Retrace"

    override fun unscramble(
        project: Project,
        text: String,
        logName: String,
        settings: JPanel?,
    ): String? {
        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName)

        if (!mappingFile.exists()) {
            ErrorDialog(project, bundle, mappingFile.name).show()
            return null
        }

        val allClassNamesSetting: Boolean =
            (settings?.getComponent(ALL_CLASS_NAMES_INDEX) as? JCheckBox)?.isSelected ?: false
        val verboseSetting: Boolean = (settings?.getComponent(VERBOSE_INDEX) as? JCheckBox)?.isSelected ?: false

        return LineNumberReader(text.reader().buffered()).use { reader ->
            val buffer = Buffer()
            PrintWriter(buffer.outputStream()).use { writer ->
                try {
                    ReTrace(
                        ReTrace.REGULAR_EXPRESSION,
                        ReTrace.REGULAR_EXPRESSION2,
                        allClassNamesSetting,
                        verboseSetting,
                        mappingFile,
                    ).retrace(
                        reader,
                        writer,
                    )
                } catch (exc: Exception) {
                    logger<ProguardRetraceUnscrambler>().error(exc)
                    with(buffer) {
                        clear()
                        writeUtf8("Error on retrace - please report to support@cmgapps.com")
                        writeUtf8("\n\n")
                        writeUtf8(exc.stackTraceToString())
                        flush()
                    }
                }
                val result = buffer.readUtf8()
                buffer.close()
                result
            }
        }
    }

    override fun createSettingsComponent() =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            JCheckBox(bundle.getString("all_class_names_text"))
                .apply {
                    isSelected = properties.getBoolean(ALL_CLASS_NAMES_PROPERTY, false)
                    addItemListener {
                        properties.setValue(ALL_CLASS_NAMES_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                    }
                }.let {
                    add(it, ALL_CLASS_NAMES_INDEX)
                }

            JCheckBox(bundle.getString("verbose_text"))
                .apply {
                    isSelected = properties.getBoolean(VERBOSE_PROPERTY, false)
                    addItemListener {
                        properties.setValue(VERBOSE_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                    }
                }.let {
                    add(it, VERBOSE_INDEX)
                }
        }

    private companion object {
        private const val ALL_CLASS_NAMES_INDEX = 0
        private const val VERBOSE_INDEX = 1
        private const val PROPERTIES_PREFIX = "com.cmgapps.intellij.proguard-retrace-unscambler"
        private const val ALL_CLASS_NAMES_PROPERTY = "$PROPERTIES_PREFIX.all-class-names"
        private const val VERBOSE_PROPERTY = "$PROPERTIES_PREFIX.verbose"
    }
}

class ErrorDialog(
    project: Project?,
    private val bundle: ResourceBundle,
    private val fileName: String,
) : DialogWrapper(project, false) {
    init {
        init()
    }

    @VisibleForTesting
    public override fun createActions(): Array<Action> = arrayOf(okAction)

    @VisibleForTesting
    public override fun createCenterPanel(): JComponent =
        JPanel(BorderLayout()).apply {
            JLabel(
                bundle.getString("error_text").format(fileName),
                Messages.getWarningIcon(),
                SwingConstants.HORIZONTAL,
            ).apply {
                iconTextGap = 10
                preferredSize = Dimension(300, 100)
            }.let {
                add(it, BorderLayout.CENTER)
            }
        }
}
