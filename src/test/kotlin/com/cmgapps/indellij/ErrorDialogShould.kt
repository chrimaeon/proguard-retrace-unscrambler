/*
 * Copyright (c) 2024. Christian Grach <christian.grach@cmgapps.com>
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

import com.cmgapps.intellij.ErrorDialog
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.arrayWithSize
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isA
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.util.ResourceBundle
import javax.swing.Action
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ErrorDialogShould : BasePlatformTestCase() {
    private val resourceBundle: ResourceBundle = ResourceBundle.getBundle("Bundle")

    private lateinit var frame: JFrame

    @BeforeEach
    override fun setUp() {
        super.setUp()
        frame = JFrame("ErrorDialog")
    }

    @AfterEach
    override fun tearDown() {
        frame.dispose()
        super.tearDown()
    }

    @Test
    fun `return one action`() {
        // whenever(resourceBundle.getString("error_text")) doReturn ""
        SwingUtilities.invokeAndWait {
            val actions =
                ErrorDialog(
                    project,
                    resourceBundle,
                    "filename.trace",
                ).createActions()

            assertThat(actions, arrayWithSize(1))
        }
    }

    @Test
    fun `return ok action`() {
        // whenever(resourceBundle.getString("error_text")) doReturn ""
        SwingUtilities.invokeAndWait {
            val actions =
                ErrorDialog(
                    project,
                    resourceBundle,
                    "filename.trace",
                ).createActions()

            assertThat(actions[0].getValue(Action.NAME), `is`("OK"))
        }
    }

    @Test
    fun `return dialog content`() {
        // whenever(resourceBundle.getString("error_text")) doReturn ""
        SwingUtilities.invokeAndWait {
            val panel =
                ErrorDialog(
                    project,
                    resourceBundle,
                    "filename.trace",
                ).createCenterPanel()

            assertThat(panel, isA(JPanel::class.java))
        }
    }

    @Test
    fun `have file name as text`() {
        val fileName = "filename.trace"
        SwingUtilities.invokeAndWait {
            frame.contentPane =
                ErrorDialog(
                    project,
                    resourceBundle,
                    fileName,
                ).createCenterPanel()

            frame.pack()
            frame.isVisible = true

            val label = frame.contentPane.findComponentAt(155, 55) as JLabel

            assertThat(label.text, startsWith(fileName))
        }
    }
}
