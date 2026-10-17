/*
 * Copyright (c) 2020. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.util.Date

plugins {
    java
    alias(libs.plugins.intellij)
    alias(libs.plugins.changelog)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    id("ktlint")
}

group = "com.cmgapps.intellij"
version =
    libs.versions.plugin.version
        .get()

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223.0"
            untilBuild = provider { null }
        }

        changeNotes =
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
            }
    }

    publishing {
        // TODO read token from env if on CI
        if (!isCi) {
            token = project.property("intellij.token") as String
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2022.3") // MIN
            create(IntelliJPlatformType.IntellijIdea, "2026.2") // LATEST
            if (!isCi) {
                create(IntelliJPlatformType.IntellijIdeaCommunity, "2023.1")
                create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1")
                create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
            }
        }
        // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#mutePluginVerifierProblems
        freeArgs =
            listOf(
                "-mute",
                "TemplateWordInPluginId",
            )
    }
}

kotlin {
    jvmToolchain(17)
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
        dependsOn(verifyPlugin, ktlint)
    }

    koverVerify {
        dependsOn(ktlint)
    }
}

val isCi: Boolean
    get() = !System.getenv("CI").isNullOrBlank()

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(Constants.Constraints.MINIMAL_INTELLIJ_PLATFORM_VERSION.toString())
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Bundled)
        testFramework(TestFrameworkType.JUnit5)
        pluginVerifier()
    }

    implementation(libs.proguard.retrace)
    implementation(libs.google.r8)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.hamcrest)
    testImplementation(libs.bundles.mockito)
    // Workaround for https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    testImplementation(libs.junit.junit)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
