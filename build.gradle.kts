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

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    java
    id("org.jetbrains.intellij") version Deps.intellijVersion
    kotlin("jvm") version Deps.kotlinVersion
    id("com.github.ben-manes.versions") version Deps.depUpdatesPluginVersion
}

group = "com.cmgapps.intellij"
version = "1.1.0"

repositories {
    jcenter()
    mavenCentral()
}

val ktlint by configurations.creating

dependencies {
    implementation(kotlin("stdlib-jdk8", Deps.kotlinVersion))
    implementation("net.sf.proguard:proguard-retrace:" + Deps.retraceVersion)
    implementation("com.squareup.okio:okio:" + Deps.okioVersion)

    ktlint("com.pinterest:ktlint:" + Deps.ktlintVersion)

    "testImplementation"(platform("org.junit:junit-bom:" + Deps.junitVersion))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testImplementation"("org.hamcrest:hamcrest-library:" + Deps.hamcrestVersion)
    "testImplementation"("org.mockito:mockito-core:" + Deps.mockitoVersion)
    "testImplementation"("org.mockito:mockito-junit-jupiter:" + Deps.mockitoVersion)
}

intellij {
    version = "2020.2"
    updateSinceUntilBuild = false
    setPlugins("java")
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "6.7"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
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
                    "Built-Kotlin" to Deps.kotlinVersion
                )
            )
        }
    }

    val ktlint by registering(JavaExec::class) {
        group = "Verification"
        description = "Check Kotlin code style."
        main = "com.pinterest.ktlint.Main"
        classpath = ktlint
        args = listOf("src/**/*.kt", "--reporter=plain", "--reporter=checkstyle,output=${buildDir}/reports/ktlint.xml")
    }

    check {
        dependsOn(ktlint)
    }

    dependencyUpdates {
        revision = "release"

        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "eap").any { qualifier ->
                """(?i).*[.-]?$qualifier[.\d-]*""".toRegex()
                    .containsMatchIn(candidate.version)
            }
        }
    }

    // region IntelliJ Plugin
    patchPluginXml {
        // changeNotes("""<b>1.0.0</b><br/>First Version""")
    }

    publishPlugin {
        // TODO read token from env if on CI
        if (System.getenv("CI") == null) {
            token(project.property("intellij.token"))
        }
    }
    // endregion
}
