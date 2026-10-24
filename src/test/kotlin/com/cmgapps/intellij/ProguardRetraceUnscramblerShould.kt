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

@file:Suppress("JUnitMixedFramework")

package com.cmgapps.intellij

import com.intellij.openapi.diagnostic.JulLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.blankString
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isA
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ResourceBundle
import java.util.zip.GZIPInputStream
import javax.swing.BoxLayout
import javax.swing.JCheckBox

class ProguardRetraceUnscramblerShould : BasePlatformTestCase() {
    private lateinit var mappingFilePath: String

    private val classLoader = javaClass.classLoader
    private lateinit var unscrambler: ProguardRetraceUnscrambler

    @BeforeEach
    fun beforeEach() {
        setUp()
        mappingFilePath = classLoader.getResource("mapping.txt")?.path ?: error("mapping.txt not found")
        unscrambler = ProguardRetraceUnscrambler()
    }

    @AfterEach
    fun afterEach() {
        tearDown()
    }

    @Test
    fun `de-obfuscate stacktrace`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = unscrambler.unscramble(project, stacktrace, mappingFilePath, DialogPanel())

        assertThat(result, `is`(classLoader.getResource("deobfuscated.txt")?.readText()))
    }

    @Test
    fun `return empty text when stacktrace is empty`() {
        val result = ProguardRetraceUnscrambler().unscramble(project, "", mappingFilePath, DialogPanel())
        assertThat(result, `is`(""))
    }

    @Test
    fun `de-obfuscate allClassNames stacktrace`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(true)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = unscrambler.unscramble(project, stacktrace, mappingFilePath, DialogPanel())

        assertThat(result, `is`(classLoader.getResource("deobfuscated-allClassNames.txt")?.readText()))
    }

    @Test
    fun `de-obfuscate verbose stacktrace`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(true)
            }
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = unscrambler.unscramble(project, stacktrace, mappingFilePath, DialogPanel())

        assertThat(result, `is`(classLoader.getResource("deobfuscated-verbose.txt")?.readText()))
    }

    @Test
    fun `de-obfuscate allClassNames and verbose stacktrace`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(true)
                unscrambler.verbose = checkBox("Verbose").selected(true)
            }
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = unscrambler.unscramble(project, stacktrace, mappingFilePath, DialogPanel())

        assertThat(result, `is`(classLoader.getResource("deobfuscated-allClassNames-verbose.txt")?.readText()))
    }

    @Test
    fun `handle stacktrace without source file reference`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val stacktraceWithoutSourcefileReference =
            "Fatal Exception: java.lang.IllegalStateException: onDismiss not set\n" +
                "    at o2.j0.w2(:49)\n" +
                "    at androidx.fragment.app.e.z2(:13)\n" +
                "    at androidx.fragment.app.e.a1(:16)\n" +
                "    at androidx.fragment.app.Fragment.C1()\n"

        val result =
            unscrambler.unscramble(
                project,
                stacktraceWithoutSourcefileReference,
                classLoader.getResource("mapping_error.txt")?.path!!,
                DialogPanel(),
            )

        assertThat(
            result,
            `is`(
                "Fatal Exception: java.lang.IllegalStateException: onDismiss not set\n" +
                    "    at o2.j0.w2(j0.java:49)\n" +
                    "    at androidx.fragment.app.DialogFragment.prepareDialog(DialogFragment.java:665)\n" +
                    "    at androidx.fragment.app.DialogFragment.onGetLayoutInflater(DialogFragment.java:579)\n" +
                    "    at androidx.fragment.app.Fragment.performGetLayoutInflater()\n",
            ),
        )
    }

    @Test
    fun `handle exception during retrace`() {
        Logger.setFactory(OmitAssertionErrorLoggerFactory::class.java)

        val errorRetracer = Retracer { throw IOException("cannot retrace on tests") }

        val unscrambler =
            ProguardRetraceUnscrambler(
                retracerFactory = { _, _, _, _, _ -> errorRetracer },
            )

        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val result =
            unscrambler.unscramble(
                project,
                "foo.bar",
                classLoader.getResource("mapping_error.txt")?.path!!,
                DialogPanel(),
            )

        assertThat(
            result,
            startsWith("Error on retrace - please report to support@cmgapps.com\n\n"),
        )
    }

    @Test
    fun `handle mapping file does not exist`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val logName = "random/foo.bar.file/does.not.exist"

        var capturesMessage: String? = null
        val previousTestDialog =
            TestDialogManager.setTestDialog { message ->
                capturesMessage = message
                0
            }
        try {
            unscrambler.unscramble(
                project,
                "foo.bar",
                logName,
                DialogPanel(),
            )
        } finally {
            TestDialogManager.setTestDialog(previousTestDialog)
        }

        assertThat(
            capturesMessage,
            `is`(
                ResourceBundle.getBundle("Bundle").getString("error_text").format(logName),
            ),
        )
    }

    @Test
    fun `return text value when logName is blank`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val text = "Stacktrace"

        val result =
            unscrambler.unscramble(
                project,
                text,
                "",
                DialogPanel(),
            )

        assertThat(
            result,
            `is`(text),
        )
    }

    @Test
    fun `return blank when text is blank`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(false)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val result =
            unscrambler.unscramble(
                project,
                " \t",
                "foo",
                DialogPanel(),
            )

        assertThat(
            result,
            `is`(blankString()),
        )
    }

    @Test
    fun `use R8`() {
        panel {
            row {
                unscrambler.useR8 = checkBox("Use R8").selected(true)
                unscrambler.allClassNames = checkBox("All Class Names").selected(false)
                unscrambler.verbose = checkBox("Verbose").selected(false)
            }
        }

        val mappingFile =
            File.createTempFile("mapping-r8", ".txt").apply {
                deleteOnExit()
            }

        GZIPInputStream(classLoader.getResourceAsStream("mapping-r8.txt.gz") ?: error("mapping-r8.txt not found")).use {
            it.copyTo(FileOutputStream(mappingFile))
        }

        val stacktrace =
            classLoader.getResource("stacktrace-r8.txt")?.readText() ?: error("stacktrace-r8.txt not found")

        val result =
            unscrambler.unscramble(
                project,
                stacktrace,
                mappingFile.absolutePath,
                DialogPanel(),
            )

        val deobfuscated =
            classLoader.getResource("deobfuscated-r8.txt")?.readText() ?: error("deobfuscated-r8.txt not found")

        assertThat(result, `is`(deobfuscated))
    }

    @Test
    fun `handle null settings`() {
        assertThrows<IllegalArgumentException> {
            ProguardRetraceUnscrambler().unscramble(project, "foo", "bar", null)
        }
    }

    @Nested
    inner class UiRelated {
        @Test
        fun `create settings panel with checkboxes`() {
            val settingsComponent = unscrambler.createSettingsComponent()

            assertThat(
                settingsComponent.components.toList(),
                hasItems(isA(JCheckBox::class.java)),
            )
        }

        @Test
        fun `return display name`() {
            assertThat(unscrambler.presentableName, `is`("Proguard / R8 Retrace"))
        }
    }
}

private class OmitAssertionErrorLoggerFactory : Logger.Factory {
    override fun getLoggerInstance(category: String): Logger =
        JulLogger(
            java.util.logging.Logger
                .getLogger("TestLogger"),
        )
}
