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

package com.cmgapps.indellij

import com.cmgapps.intellij.ProguardRetraceUnscrambler
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isA
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

@ExtendWith(MockitoExtension::class)
class ProguardRetraceUnscramblerShould : BasePlatformTestCase() {
    private lateinit var mappingFilePath: String

    private lateinit var settings: JPanel

    private val classLoader = javaClass.classLoader

    @BeforeEach
    fun beforeEach() {
        setUp()
        mappingFilePath = classLoader.getResource("mapping.txt")?.path ?: error("mapping.txt not found")
        settings = JPanel()
    }

    @AfterEach
    fun afterEach() {
        tearDown()
    }

    @Test
    fun `de-obfuscate stacktrace`() {
        settings.apply {
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                0,
            )
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                1,
            )
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = ProguardRetraceUnscrambler().unscramble(project, stacktrace, mappingFilePath, settings)

        assertThat(result, `is`(classLoader.getResource("deobfuscated.txt")?.readText()))
    }

    @Test
    fun `return empty text when stacktrace is empty`() {
        val result = ProguardRetraceUnscrambler().unscramble(project, "", mappingFilePath, settings)
        assertThat(result, `is`(""))
    }

    @Test
    fun `de-obfuscate allClassNames stacktrace`() {
        settings.apply {
            add(
                JCheckBox().also {
                    it.isSelected = true
                },
                0,
            )
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                1,
            )
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = ProguardRetraceUnscrambler().unscramble(project, stacktrace, mappingFilePath, settings)

        assertThat(result, `is`(classLoader.getResource("deobfuscated-allClassNames.txt")?.readText()))
    }

    @Test
    fun `de-obfuscate verbose stacktrace`() {
        settings.apply {
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                0,
            )
            add(
                JCheckBox().also {
                    it.isSelected = true
                },
                1,
            )
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = ProguardRetraceUnscrambler().unscramble(project, stacktrace, mappingFilePath, settings)

        assertThat(result, `is`(classLoader.getResource("deobfuscated-verbose.txt")?.readText()))
    }

    @Test
    fun `de-obfuscate allClassNames and verbose stacktrace`() {
        settings.apply {
            add(
                JCheckBox().also {
                    it.isSelected = true
                },
                0,
            )
            add(
                JCheckBox().also {
                    it.isSelected = true
                },
                1,
            )
        }
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")

        val result = ProguardRetraceUnscrambler().unscramble(project, stacktrace, mappingFilePath, settings)

        assertThat(result, `is`(classLoader.getResource("deobfuscated-allClassNames-verbose.txt")?.readText()))
    }

    @Test
    fun `handle stacktrace without source file reference`() {
        settings.apply {
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                0,
            )
            add(
                JCheckBox().also {
                    it.isSelected = false
                },
                1,
            )
        }

        val stacktraceWithoutSourcefileReference =
            "Fatal Exception: java.lang.IllegalStateException: onDismiss not set\n" +
                "    at o2.j0.w2(:49)\n" +
                "    at androidx.fragment.app.e.z2(:13)\n" +
                "    at androidx.fragment.app.e.a1(:16)\n" +
                "    at androidx.fragment.app.Fragment.C1()\n"

        val result =
            ProguardRetraceUnscrambler().unscramble(
                project,
                stacktraceWithoutSourcefileReference,
                classLoader.getResource("mapping_error.txt")?.path!!,
                settings,
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

    @Nested
    inner class UiRelated {
        @Test
        fun `create settings panel with box layout`() {
            val settingsComponent = ProguardRetraceUnscrambler().createSettingsComponent()
            assertThat(settingsComponent.layout, isA(BoxLayout::class.java))
        }

        @Test
        fun `create settings panel with 2 items`() {
            val settingsComponent = ProguardRetraceUnscrambler().createSettingsComponent()
            assertThat(settingsComponent.componentCount, `is`(2))
        }

        @Test
        fun `create settings panel with checkboxes`() {
            val settingsComponent = ProguardRetraceUnscrambler().createSettingsComponent()
            assertThat(
                settingsComponent.components.toList(),
                hasItems(isA(JCheckBox::class.java)),
            )
        }

        @Test
        fun `return display name`() {
            assertThat(ProguardRetraceUnscrambler().presentableName, `is`("Proguard Retrace"))
        }
    }
}
