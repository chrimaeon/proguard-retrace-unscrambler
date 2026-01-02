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

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.unscramble.UnscrambleSupport
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.ResourceBundle
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JLabel

typealias RetracerFactory = (
    mappingFile: File,
    logger: Logger,
    verbose: Boolean,
    allClassNames: Boolean,
    useR8: Boolean,
) -> Retracer

private val DEFAULT_RETRACER_FACTORY: RetracerFactory = { mappingFile, logger, verbose, allClassNames, useR8 ->
    Retracer.create(
        mappingFile = mappingFile,
        logger = logger,
        verbose = verbose,
        allClassNames = allClassNames,
        useR8 = useR8,
    )
}

class ProguardRetraceUnscrambler(
    private val retracerFactory: RetracerFactory = DEFAULT_RETRACER_FACTORY,
) : UnscrambleSupport<Box> {
    private val bundle = ResourceBundle.getBundle("Bundle")
    private val properties = PropertiesComponent.getInstance()

    override fun getPresentableName() = "Proguard / R8 Retrace"

    override fun unscramble(
        project: Project,
        text: String,
        logName: String,
        settings: Box?,
    ): String? {
        requireNotNull(settings) { "Settings cannot be null" }

        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName)

        if (!mappingFile.exists()) {
            Messages.showErrorDialog(project, bundle.getString("error_text").format(logName), "")
            return null
        }

        val r8SettingsPanel = (settings.getComponent(USE_R8_INDEX) as Box)

        val useR8: Boolean =
            (r8SettingsPanel.getComponent(0) as JCheckBox).isSelected

        val allClassNamesSetting: Boolean =
            (settings.getComponent(ALL_CLASS_NAMES_INDEX) as JCheckBox).isSelected

        val verboseSetting: Boolean = (settings.getComponent(VERBOSE_INDEX) as JCheckBox).isSelected

        val logger = logger<ProguardRetraceUnscrambler>()

        return try {
            retracerFactory(
                mappingFile,
                logger,
                verboseSetting,
                allClassNamesSetting,
                useR8,
            ).retraceStackTrace(text)
        } catch (exc: Exception) {
            logger.error(exc)
            """
               |Error on retrace - please report to support@cmgapps.com
               |
               |${exc.stackTraceToString()}
            """.trimMargin()
        }
    }

    override fun createSettingsComponent(): Box =
        Box.createVerticalBox().let { container ->
            container.alignmentX = Component.LEFT_ALIGNMENT
            val useR8Label = bundle.getString("use_r8")

            val useR8 =
                JCheckBox(useR8Label, Messages.getInformationIcon(), properties.getBoolean(USE_R8_PROPERTY, false))
                    .apply {
                        addItemListener {
                            properties.setValue(USE_R8_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                        }
                    }

            Box
                .createHorizontalBox()
                .apply {
                    alignmentX = Component.LEFT_ALIGNMENT

                    add(useR8)
                    add(Box.createRigidArea(Dimension(5, 0)))
                    add(
                        JLabel(
                            AllIcons.General.Note,
                        ).apply {
                            addMouseListener(
                                object : MouseAdapter() {
                                    override fun mouseClicked(e: MouseEvent) {
                                        if (e.button == MouseEvent.BUTTON1) {
                                            Messages.showMessageDialog(
                                                bundle.getString("use_r8_note"),
                                                useR8Label,
                                                Messages.getInformationIcon(),
                                            )
                                        }
                                    }
                                },
                            )
                        },
                    )
                }.let(container::add)

            JCheckBox(bundle.getString("verbose_text"), properties.getBoolean(VERBOSE_PROPERTY, false))
                .apply {
                    addItemListener {
                        properties.setValue(VERBOSE_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                    }
                }.let {
                    container.add(it)
                }

            val allClassNames =
                JCheckBox(
                    bundle.getString("all_class_names_text"),
                    properties.getBoolean(ALL_CLASS_NAMES_PROPERTY, false),
                ).apply {
                    isEnabled = !useR8.isSelected
                    addItemListener {
                        properties.setValue(ALL_CLASS_NAMES_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                    }
                }
            container.add(allClassNames)

            useR8.addItemListener {
                allClassNames.isEnabled = it.stateChange != ItemEvent.SELECTED
            }
            container
        }

    private companion object {
        private const val USE_R8_INDEX = 0
        private const val VERBOSE_INDEX = 1
        private const val ALL_CLASS_NAMES_INDEX = 2
        private const val PROPERTIES_PREFIX = "com.cmgapps.intellij.proguard-retrace-unscrambler"
        private const val ALL_CLASS_NAMES_PROPERTY = "$PROPERTIES_PREFIX.all-class-names"
        private const val VERBOSE_PROPERTY = "$PROPERTIES_PREFIX.verbose"
        private const val USE_R8_PROPERTY = "$PROPERTIES_PREFIX.use_r8"
    }
}
