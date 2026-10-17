/*
 * Copyright (c) 2020. Christian Grach <christian.grach@cmgapps.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import de.fayard.refreshVersions.core.FeatureFlag
import de.fayard.refreshVersions.core.StabilityLevel

rootProject.name = "Proguard Retrace Unscrambler"

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.6"
}

refreshVersions {
    featureFlags {
        enable(FeatureFlag.GRADLE_UPDATES)
    }

    rejectVersionIf {
        candidate.stabilityLevel != StabilityLevel.Stable
    }
}
