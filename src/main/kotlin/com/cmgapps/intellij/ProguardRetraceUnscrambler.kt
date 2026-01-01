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

import com.android.tools.r8.Diagnostic
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.retrace.ProguardMapProducer
import com.android.tools.r8.retrace.ProguardMappingSupplier
import com.android.tools.r8.retrace.RetraceStackTraceContext
import com.android.tools.r8.retrace.StringRetrace
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.unscramble.UnscrambleSupport
import org.jetbrains.annotations.VisibleForTesting
import proguard.retrace.ReTrace
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.LineNumberReader
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ResourceBundle
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

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

typealias ErrorDialogFactory = (
    project: Project,
    bundle: ResourceBundle,
    mappingFile: File,
) -> DialogWrapper

private val DEFAULT_ERROR_DIALOG_FACTORY: ErrorDialogFactory = { project, bundle, mappingFile ->
    ErrorDialog(
        project,
        bundle,
        mappingFile.name,
    )
}

class ProguardRetraceUnscrambler(
    private val retracerFactory: RetracerFactory = DEFAULT_RETRACER_FACTORY,
    private val errorDialogFactory: ErrorDialogFactory = DEFAULT_ERROR_DIALOG_FACTORY,
) : UnscrambleSupport<JPanel> {
    private val bundle = ResourceBundle.getBundle("Bundle")
    private val properties = PropertiesComponent.getInstance()

    override fun getPresentableName() = "Proguard / R8 Retrace"

    override fun unscramble(
        project: Project,
        text: String,
        logName: String,
        settings: JPanel?,
    ): String? {
        if (logName.isBlank() || text.isBlank()) return text

        val mappingFile = File(logName)

        if (!mappingFile.exists()) {
            errorDialogFactory(project, bundle, mappingFile).show()
            return null
        }

        val useR8: Boolean = (settings?.getComponent(USE_R8_INDEX) as? JCheckBox)?.isSelected ?: false

        val allClassNamesSetting: Boolean =
            (settings?.getComponent(ALL_CLASS_NAMES_INDEX) as? JCheckBox)?.isSelected ?: false
        val verboseSetting: Boolean = (settings?.getComponent(VERBOSE_INDEX) as? JCheckBox)?.isSelected ?: false

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

    override fun createSettingsComponent() =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val useR8 =
                JCheckBox(bundle.getString("use_r8"))
                    .apply {
                        isSelected = properties.getBoolean(USE_R8_PROPERTY, false)
                        addItemListener {
                            properties.setValue(USE_R8_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                        }
                    }
            add(useR8, USE_R8_INDEX)

            JCheckBox(bundle.getString("verbose_text"))
                .apply {
                    isSelected = properties.getBoolean(VERBOSE_PROPERTY, false)
                    addItemListener {
                        properties.setValue(VERBOSE_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                    }
                }.let {
                    add(it, VERBOSE_INDEX)
                }

            val allClassNames =
                JCheckBox(bundle.getString("all_class_names_text"))
                    .apply {
                        isEnabled = !useR8.isSelected
                        isSelected = properties.getBoolean(ALL_CLASS_NAMES_PROPERTY, false)
                        addItemListener {
                            properties.setValue(ALL_CLASS_NAMES_PROPERTY, it.stateChange == ItemEvent.SELECTED)
                        }
                    }
            add(allClassNames, ALL_CLASS_NAMES_INDEX)

            useR8.addItemListener {
                allClassNames.isEnabled = it.stateChange != ItemEvent.SELECTED
            }
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

class ErrorDialog(
    project: Project,
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

private class R8Retracer(
    private val retrace: StringRetrace,
) : Retracer {
    override fun retraceStackTrace(stacktrace: String): String {
        val parsedStacktrace =
            LineNumberReader(stacktrace.reader()).use { reader ->
                reader.readLines()
            }

        val writer = StringWriter()
        writer.use {
            retrace
                .retraceStackTrace(parsedStacktrace, RetraceStackTraceContext.empty())
                .result
                .forEach { frameResults ->
                    frameResults.ambiguousResult.forEach {
                        writer.append(it.result.joinToString(separator = "\n")).append("\n")
                    }
                }
        }

        return writer.toString()
    }

    companion object {
        // This is a slight modification of the default regular expression shown for proguard-retrace
        // that allows for retracing classes in the form <class>: lorem ipsum...
        // Seems like Proguard retrace is expecting the form "Caused by: <class>".
        // com/android/tools/r8/retrace/internal/StackTraceRegularExpressionParser.java
        const val DEFAULT_REGULAR_EXPRESSION =
            "(?:.*?\\bat\\s+%c\\.%m\\s*\\(%S\\)\\p{Z}*(?:~\\[.*\\])?)" +
                "|(?:(?:(?:%c|.*)?[:\"]\\s+)?%c(?:(:|]).*)?)"
    }
}

private class ProguardRetracer(
    private val retrace: ReTrace,
) : Retracer {
    override fun retraceStackTrace(stacktrace: String): String {
        val buffer = ByteArrayOutputStream()
        LineNumberReader(stacktrace.reader().buffered()).use { reader ->
            PrintWriter(buffer).use { writer ->
                retrace.retrace(reader, writer)
            }
        }
        return buffer.toString()
    }
}

fun interface Retracer {
    fun retraceStackTrace(stacktrace: String): String

    companion object {
        fun create(
            mappingFile: File,
            logger: Logger,
            verbose: Boolean,
            allClassNames: Boolean,
            useR8: Boolean,
        ): Retracer {
            if (useR8) {
                logger.info("Using R8 Retrace")
                return R8Retracer(
                    StringRetrace.create(
                        ProguardMappingSupplier
                            .builder()
                            .setProguardMapProducer(ProguardMapProducer.fromPath(mappingFile.toPath()))
                            .build(),
                        R8DiagnosticsHandler(logger),
                        R8Retracer.DEFAULT_REGULAR_EXPRESSION,
                        verbose,
                    ),
                )
            }

            logger.info("Using Proguard Retrace")

            return ProguardRetracer(
                ReTrace(
                    ReTrace.REGULAR_EXPRESSION,
                    ReTrace.REGULAR_EXPRESSION2,
                    allClassNames,
                    verbose,
                    mappingFile,
                ),
            )
        }
    }
}

internal class R8DiagnosticsHandler(
    private val logger: Logger,
) : DiagnosticsHandler {
    override fun error(error: Diagnostic) {
        ByteArrayOutputStream().use {
            DiagnosticsHandler.printDiagnosticToStream(error, "R8 Retrace Error", PrintStream(it))
            logger.error(it.toString())
        }
    }

    override fun warning(error: Diagnostic) {
        ByteArrayOutputStream().use {
            DiagnosticsHandler.printDiagnosticToStream(error, "R8 Retrace Warning", PrintStream(it))
            logger.warn(it.toString())
        }
    }

    override fun info(error: Diagnostic) {
        ByteArrayOutputStream().use {
            DiagnosticsHandler.printDiagnosticToStream(error, "R8 Retrace Info", PrintStream(it))
            logger.info(it.toString())
        }
    }
}
