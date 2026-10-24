/*
 * Copyright (c) 2020. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.intellij

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.unscramble.UnscrambleSupport
import java.io.File
import java.util.ResourceBundle
import javax.swing.JCheckBox

typealias RetracerFactory = (
    mappingFile: File,
    logger: Logger,
    verbose: Boolean,
    allClassNames: Boolean,
    useR8: Boolean,
) -> Retracer

private val DEFAULT_RETRACER_FACTORY: RetracerFactory = { mappingFile, logger, verbose, allClassNames, useR8 ->
    Retracer(
        mappingFile = mappingFile,
        logger = logger,
        verbose = verbose,
        allClassNames = allClassNames,
        useR8 = useR8,
    )
}

class ProguardRetraceUnscrambler(
    private val retracerFactory: RetracerFactory = DEFAULT_RETRACER_FACTORY,
) : UnscrambleSupport<DialogPanel> {
    private val bundle = ResourceBundle.getBundle("Bundle")
    private val properties = PropertiesComponent.getInstance()

    lateinit var useR8: Cell<JCheckBox>
    lateinit var verbose: Cell<JCheckBox>
    lateinit var allClassNames: Cell<JCheckBox>

    override fun getPresentableName() = "Proguard / R8 Retrace"

    override fun unscramble(
        project: Project,
        text: String,
        logName: String,
        settings: DialogPanel?,
    ): String? {
        requireNotNull(settings) { "Settings cannot be null" }

        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName)

        if (!mappingFile.exists()) {
            val message = bundle.getString("error_text").format(logName)
            ApplicationManager.getApplication().invokeAndWait(
                {
                    if (!project.isDisposed) {
                        Messages.showErrorDialog(project, message, "")
                    }
                },
                ModalityState.defaultModalityState(),
            )
            return null
        }

        val useR8: Boolean = useR8.component.isSelected
        val verboseSetting: Boolean = verbose.component.isSelected
        val allClassNamesSetting: Boolean = allClassNames.component.isSelected

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

    override fun createSettingsComponent(): DialogPanel =
        panel {
            row {
                useR8 =
                    checkBox(bundle.getString("use_r8"))
                        .bindSelected(
                            getter = { properties.getBoolean(USE_R8_PROPERTY, false) },
                            setter = { properties.setValue(USE_R8_PROPERTY, it) },
                        ).gap(RightGap.SMALL)
                val action =
                    object : AnAction(
                        AllIcons.General.ContextHelp,
                    ) {
                        override fun actionPerformed(e: AnActionEvent) {
                            Messages.showMessageDialog(
                                bundle.getString("use_r8_note"),
                                bundle.getString("use_r8"),
                                Messages.getInformationIcon(),
                            )
                        }
                    }
                actionButton(action)
            }
            row {
                verbose =
                    checkBox(bundle.getString("verbose_text"))
                        .bindSelected(
                            getter = { properties.getBoolean(VERBOSE_PROPERTY, false) },
                            setter = { properties.setValue(VERBOSE_PROPERTY, it) },
                        )
            }
            row {
                allClassNames =
                    checkBox(bundle.getString("all_class_names_text"))
                        .bindSelected(
                            getter = { properties.getBoolean(ALL_CLASS_NAMES_PROPERTY, false) },
                            setter = { properties.setValue(ALL_CLASS_NAMES_PROPERTY, it) },
                        )
            }.enabledIf(!useR8.selected)
        }

    private companion object {
        private const val PROPERTIES_PREFIX = "com.cmgapps.intellij.proguard-retrace-unscrambler"
        private const val ALL_CLASS_NAMES_PROPERTY = "$PROPERTIES_PREFIX.all-class-names"
        private const val VERBOSE_PROPERTY = "$PROPERTIES_PREFIX.verbose"
        private const val USE_R8_PROPERTY = "$PROPERTIES_PREFIX.use_r8"
    }
}
