/*
 * Copyright (c) 2026. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.intellij

import com.android.tools.r8.Diagnostic
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.retrace.ProguardMapProducer
import com.android.tools.r8.retrace.ProguardMappingSupplier
import com.android.tools.r8.retrace.RetraceStackTraceContext
import com.android.tools.r8.retrace.StringRetrace
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.VisibleForTesting
import proguard.retrace.ReTrace
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.LineNumberReader
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

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

@VisibleForTesting
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
