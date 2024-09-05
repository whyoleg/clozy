/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

import com.vanniktech.maven.publish.*
import kotlinx.validation.*
import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    id("clozybuild.kotlin")
    id("clozybuild.publication")
    id("clozybuild.documentation")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.bcv)
}

description = "clozy"

mavenPublishing {
    configure(KotlinMultiplatform())
}

apiValidation {
    @OptIn(ExperimentalBCVApi::class)
    klib.enabled = true
}

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    explicitApi()

    // cleaner is available from JDK 9 so we can't use JDK 8
    jvmToolchain(21)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
        testRuns.named("test") {
            this.executionTask.configure {
                // TODO: make it dependsOn other test tasks
                onlyIf("Use target specific task") { false }
            }
        }
        setOf(8, 11, 17, 21).forEach { jdkTestVersion ->
            testRuns.create("${jdkTestVersion}Test") {
                executionTask.configure {
                    javaLauncher.set(javaToolchains.launcherFor {
                        languageVersion.set(JavaLanguageVersion.of(jdkTestVersion))
                    })
                }
            }
        }
    }
    js {
        nodejs()
        browser()
    }
    wasmJs {
        nodejs()
        browser()
    }
    wasmWasi {
        nodejs()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    macosX64()
    macosArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    androidNativeX64()
    androidNativeX86()
    androidNativeArm64()
    androidNativeArm32()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs/api"))
}
