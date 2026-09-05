import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    // Android placeholder: uncomment when SDK is available
    // alias(libs.plugins.androidLibrary)
}

kotlin {
    // Android placeholder — keep source set for neat structure, enable when SDK present
    // androidTarget()

    jvm()
    // KCEF/Chromium requires JetBrains Runtime — Temurin loops restart forever.
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.jna)
                implementation(libs.webview)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        // androidMain is a placeholder source set (neat folder) — not compiled until androidTarget() enabled
        // webMain is an alias for file-existence check per Task 1 spec; actual compiled source is wasmJsMain
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            // JetBrains Mono is loaded via composeResources + fontFamily — black & white theme prep
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.chikura.app.MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "com.chikura.app"
                packageVersion = "1.0.0"
            }
        }
    }
}

// KCEF (Chromium) needs these flags on every desktop JVM launch.
afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
    }
}

// Compose resources — black & white theme prep (JetBrains Mono)
// Files in src/commonMain/composeResources will be available via Res
// Android placeholder — neat folder app/composeApp/src/androidMain exists
// To enable Android, uncomment in plugins: alias(libs.plugins.androidLibrary)
// and kotlin { androidTarget() }, then restore android { namespace ... } block
// android {
//     compileSdk = 34
//     namespace = "com.chikura.app"
// }
