/*
 * Copyright (c) 2023. Christian Grach <christian.grach@cmgapps.com>
 */

@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(
    ":plugins",
)
