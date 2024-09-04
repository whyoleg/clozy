/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

import clozysettings.*

pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-settings")
}

plugins {
    id("clozysettings.default")
}

projects("clozy") {
    module("clozy-core")
}
