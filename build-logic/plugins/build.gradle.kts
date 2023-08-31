/*
 * Copyright (c) 2023. Christian Grach <christian.grach@cmgapps.com>
 */

plugins {
    `kotlin-dsl`
}

group = "com.cmgapps.gradle.phonews.buildlogic.plugins"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
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
