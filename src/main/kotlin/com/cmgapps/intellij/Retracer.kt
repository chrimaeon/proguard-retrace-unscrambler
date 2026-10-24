/*
 * Copyright (c) 2026. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.cmgapps.intellij

import com.android.tools.r8.Diagnostic
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.retrace.ProguardMapProducer
import com.android.tools.r8.retrace.RetraceOptions
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

@FunctionalInterface
fun interface Retracer {
    fun retraceStackTrace(stacktrace: String): String

    companion object {
        operator fun invoke(
            mappingFile: File,
            logger: Logger,
            verbose: Boolean,
            allClassNames: Boolean,
            useR8: Boolean,
        ): Retracer =
            when {
                useR8 -> {
                    logger.info("Using R8 Retrace")
                    R8Retracer(
                        StringRetrace.create(
                            RetraceOptions
                                .builder(R8DiagnosticsHandler(logger))
                                .setProguardMapProducer(ProguardMapProducer.fromPath(mappingFile.toPath()))
                                .setVerbose(verbose)
                                .build(),
                        ),
                    )
                }

                else -> {
                    logger.info("Using Proguard Retrace")
                    ProguardRetracer(
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
        override fun retraceStackTrace(stacktrace: String): String =
            retrace
                .retraceStackTrace(stacktrace.lineSequence().toList(), RetraceStackTraceContext.empty())
                .result
                .flatMap { it.ambiguousResult }
                .joinToString(separator = "\n") { it.result.joinToString(separator = "\n") }
    }
}

private class ProguardRetracer(
    private val retrace: ReTrace,
) : Retracer {
    override fun retraceStackTrace(stacktrace: String): String =
        ByteArrayOutputStream()
            .also { buffer ->
                LineNumberReader(stacktrace.reader()).use { reader ->
                    PrintWriter(buffer).use { writer ->
                        retrace.retrace(reader, writer)
                    }
                }
            }.toString()
}

@VisibleForTesting
internal class R8DiagnosticsHandler(
    private val logger: Logger,
) : DiagnosticsHandler {
    override fun error(error: Diagnostic) = log(error, "R8 Retrace Error", logger::error)

    override fun warning(warning: Diagnostic) = log(warning, "R8 Retrace Warning", logger::warn)

    override fun info(info: Diagnostic) = log(info, "R8 Retrace Info", logger::info)

    private fun log(
        diagnostic: Diagnostic,
        title: String,
        logMessage: (String) -> Unit,
    ) {
        val output = ByteArrayOutputStream()
        PrintStream(output).use { stream ->
            DiagnosticsHandler.printDiagnosticToStream(diagnostic, title, stream)
        }
        logMessage(output.toString(Charsets.UTF_8))
    }
}
