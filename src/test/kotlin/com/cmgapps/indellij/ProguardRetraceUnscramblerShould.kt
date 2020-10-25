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
import com.intellij.openapi.project.Project
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ProguardRetraceUnscramblerShould {

    @Mock
    lateinit var project: Project

    private lateinit var mappingFilePath: String

    private val classLoader = javaClass.classLoader

    @BeforeEach
    fun setup() {
        mappingFilePath = classLoader.getResource("mapping.txt")?.path ?: error("mapping.txt not found")
    }

    @Test
    fun `deobfuscate stacktrace`() {
        val stacktrace = classLoader.getResource("stacktrace.txt")?.readText() ?: error("stack.trace not found")
        val result = ProguardRetraceUnscrambler().unscramble(project, stacktrace, mappingFilePath, null)
        assertThat(result, `is`(classLoader.getResource("deobfuscated.txt")?.readText()))
    }

    @Test
    fun `return empty text when stacktrace is empty`() {
        val result = ProguardRetraceUnscrambler().unscramble(project, "", mappingFilePath, null)
        assertThat(result, `is`(""))
    }

    @Test
    fun `return null when mapping file does not exist`() {
        val result = ProguardRetraceUnscrambler().unscramble(project, "", "path/to/nowhere", null)
        assertThat(result, nullValue())
    }
}
