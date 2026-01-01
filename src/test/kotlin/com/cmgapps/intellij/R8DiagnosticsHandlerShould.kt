/*
 * Copyright (c) 2026. Christian Grach <christian.grach@cmgapps.com>
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
import com.android.tools.r8.origin.Origin
import com.android.tools.r8.origin.PathOrigin
import com.android.tools.r8.position.Position
import com.android.tools.r8.position.TextPosition
import com.intellij.openapi.diagnostic.Logger
import org.apache.log4j.Level
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringWriter
import java.util.Formatter

class R8DiagnosticsHandlerShould {
    private lateinit var stringWriter: StringWriter
    private val logger =
        object : Logger() {
            override fun isDebugEnabled(): Boolean = true

            override fun debug(message: String?) {
                stringWriter.append(message ?: "")
            }

            override fun debug(error: Throwable?) {
                stringWriter.append(error?.message ?: "")
            }

            override fun debug(
                message: String?,
                error: Throwable?,
            ) {
                stringWriter.append(message ?: "")
            }

            override fun info(message: String?) {
                stringWriter.append(message ?: "")
            }

            override fun info(
                message: String?,
                error: Throwable?,
            ) {
                stringWriter.append(message ?: "")
            }

            override fun warn(
                message: String?,
                error: Throwable?,
            ) {
                stringWriter.append(message ?: "")
            }

            override fun error(
                message: String?,
                error: Throwable?,
                vararg args: String?,
            ) {
                stringWriter.append(Formatter().format(message ?: "", *args).toString())
            }

            @Deprecated("Deprecated in Java")
            override fun setLevel(level: Level) {
                // NO-OP
            }
        }

    private lateinit var diagnosticsHandler: R8DiagnosticsHandler

    @BeforeEach
    fun setup() {
        stringWriter = StringWriter()
        diagnosticsHandler = R8DiagnosticsHandler(logger)
    }

    @Test
    fun `report error`() {
        val diagnostic =
            object : Diagnostic {
                override fun getOrigin(): Origin? = Origin.unknown()

                override fun getPosition(): Position? = Position.UNKNOWN

                override fun getDiagnosticMessage(): String = "This is the diagnostic message"
            }
        diagnosticsHandler.error(diagnostic)
        assertThat(stringWriter.toString(), `is`("R8 Retrace Error: This is the diagnostic message\n"))
    }

    @Test
    fun `report warning`() {
        val diagnostic =
            object : Diagnostic {
                override fun getOrigin(): Origin? = Origin.unknown()

                override fun getPosition(): Position? = Position.UNKNOWN

                override fun getDiagnosticMessage(): String = "This is the diagnostic message"
            }
        diagnosticsHandler.warning(diagnostic)
        assertThat(stringWriter.toString(), `is`("R8 Retrace Warning: This is the diagnostic message\n"))
    }

    @Test
    fun `report info`() {
        val diagnostic =
            object : Diagnostic {
                override fun getOrigin(): Origin? = Origin.unknown()

                override fun getPosition(): Position? = Position.UNKNOWN

                override fun getDiagnosticMessage(): String = "This is the diagnostic message"
            }
        diagnosticsHandler.info(diagnostic)
        assertThat(stringWriter.toString(), `is`("R8 Retrace Info: This is the diagnostic message\n"))
    }

    @Test
    fun `handle origin and position`() {
        val diagnostic =
            object : Diagnostic {
                override fun getOrigin(): Origin = PathOrigin(File("path/to/file.txt").toPath())

                override fun getPosition(): Position = TextPosition(0, 2, 42)

                override fun getDiagnosticMessage(): String = "This is the diagnostic message"
            }
        diagnosticsHandler.info(diagnostic)
        assertThat(
            stringWriter.toString(),
            `is`(
                "R8 Retrace Info in path/to/file.txt at line 2, column 42:\n" +
                    "This is the diagnostic message\n",
            ),
        )
    }
}
