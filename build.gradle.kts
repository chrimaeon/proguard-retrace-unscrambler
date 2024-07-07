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
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.Changelog
import java.util.Date

plugins {
    java
    alias(libs.plugins.intellij)
    alias(libs.plugins.changelog)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.depUpdates)
    alias(libs.plugins.kover)
    id("ktlint")
}

group = "com.cmgapps.intellij"
version = "1.9.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1")
    updateSinceUntilBuild.set(false)
    plugins.add("java")
}

kotlin {
    jvmToolchain(8)
}

kover {
    reports {
        total {
            html {
                onCheck = true
            }

            verify {
                onCheck = true

                rule {
                    bound {
                        minValue = 80
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

changelog {
    header.set(
        provider {
            version.get()
        },
    )
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = libs.versions.gradle.get()
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Build-Date" to Date(),
                    "Build-By" to System.getProperty("user.name"),
                    "Build-Jdk" to System.getProperty("java.version"),
                    "Build-Gradle" to gradle.gradleVersion,
                    "Build-Kotlin" to libs.versions.kotlin.get(),
                ),
            )
        }
    }

    check {
        dependsOn(verifyPlugin)
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = GradleReleaseChannel.CURRENT.id

        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "eap").any { qualifier ->
                """(?i).*[.-]?$qualifier[.\d-]*"""
                    .toRegex()
                    .containsMatchIn(candidate.version)
            }
        }
    }

    koverVerify {
        dependsOn(ktlint)
    }

    // region IntelliJ Plugin
    patchPluginXml {
        sinceBuild.set("201")
        changeNotes.set(
            provider {
                val item =
                    if (changelog.has(project.version as String)) {
                        changelog.get(project.version as String)
                    } else {
                        changelog.getUnreleased()
                    }
                changelog.renderItem(
                    item
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            },
        )
    }

    publishPlugin {
        // TODO read token from env if on CI
        if (!isCi) {
            token.set(project.property("intellij.token") as String)
        }
    }

    runPluginVerifier {
        val ideToVerify =
            buildList {
                addAll(
                    listOf(
                        "IC-2023.1",
                        // latest
                        "IC-2024.1",
                    ),
                )

                if (!isCi) {
                    // test all locally
                    addAll(
                        listOf(
                            "IC-2020.1.4",
                            "IC-2021.1.3",
                            "IC-2022.3",
                        ),
                    )
                }
            }
        ideVersions.addAll(ideToVerify)
    }

    buildSearchableOptions {
        enabled = false
    }
    // endregion
}

val isCi: Boolean
    get() = !System.getenv("CI").isNullOrBlank()

dependencies {
    implementation(libs.proguard.retrace)
    implementation(libs.okio)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation(libs.hamcrest)
    testImplementation(libs.bundles.mockito)
}
