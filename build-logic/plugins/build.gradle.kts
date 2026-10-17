/*
 * Copyright (c) 2023. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
}

group = "com.cmgapps.gradle.phonews.buildlogic.plugins"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        register("ktlintPlugin") {
            id = "ktlint"
            implementationClass = "KtlintPlugin"
        }
    }
}
