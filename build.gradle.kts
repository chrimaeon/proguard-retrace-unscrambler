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

import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel
import kotlinx.kover.api.VerificationValueType.COVERED_LINES_PERCENTAGE
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    java
    alias(libs.plugins.intellij)
    alias(libs.plugins.changelog)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.depUpdates)
    alias(libs.plugins.kover)
}

group = "com.cmgapps.intellij"
version = "1.4.0"

repositories {
    mavenCentral()
}

val ktlint: Configuration by configurations.creating

intellij {
    version.set("2022.1")
    updateSinceUntilBuild.set(false)
    plugins.add("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = libs.versions.gradle.get()
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Built-By" to System.getProperty("user.name"),
                    "Built-Date" to Date(),
                    "Built-JDK" to System.getProperty("java.version"),
                    "Built-Gradle" to gradle.gradleVersion,
                    "Built-Kotlin" to libs.versions.kotlin.get()
                )
            )
        }
    }

    val ktlint by registering(JavaExec::class) {
        group = "Verification"
        description = "Check Kotlin code style."
        mainClass.set("com.pinterest.ktlint.Main")
        classpath = ktlint
        args = listOf(
            "src/**/*.kt",
            "--reporter=plain",
            "--reporter=checkstyle,output=$buildDir/reports/ktlint.xml"
        )
    }

    check {
        dependsOn(ktlint)
        dependsOn(verifyPlugin)
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = GradleReleaseChannel.CURRENT.id

        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "eap").any { qualifier ->
                """(?i).*[.-]?$qualifier[.\d-]*""".toRegex()
                    .containsMatchIn(candidate.version)
            }
        }
    }

    koverMergedHtmlReport {
        excludes = listOf("com.cmgapps.intellij.ErrorDialog")
    }

    koverMergedVerify {
        excludes = listOf("com.cmgapps.intellij.ErrorDialog")

        rule {
            name = "Minimal line coverage rate in percent"
            bound {
                minValue = 80
                valueType = COVERED_LINES_PERCENTAGE
            }
        }
    }

    // region IntelliJ Plugin
    patchPluginXml {
        changeNotes.set(
            provider {
                if (changelog.has(project.version as String)) {
                    changelog.get(project.version as String)
                } else {
                    changelog.getUnreleased()
                }.toHTML()
            }
        )
    }

    publishPlugin {
        // TODO read token from env if on CI
        if (System.getenv("CI") == null) {
            token.set(project.property("intellij.token") as String)
        }
    }

    runPluginVerifier {
        ideVersions.addAll(
            "IC-2018.1.8",
            "IC-2019.1.4",
            "IC-2020.1.4",
            "IC-2021.1.3",
            "IC-2022.1.1",
        )
    }
    // endregion
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.proguard.retrace)
    implementation(libs.okio)

    ktlint(libs.ktlint)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.hamcrest)
    testImplementation(libs.bundles.mockito)
}
