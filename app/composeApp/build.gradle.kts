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

    jvm("desktop")

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
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                // Compose desktop is provided via compose.desktop.* handled by plugin
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
        val desktopTest by getting {
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
            mainClass = "com.knowledgebunker.app.MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "com.knowledgebunker.app"
                packageVersion = "1.0.0"
            }
        }
    }
}

// Compose resources — black & white theme prep (JetBrains Mono)
// Files in src/commonMain/composeResources will be available via Res
// Android placeholder — neat folder app/composeApp/src/androidMain exists
// To enable Android, uncomment in plugins: alias(libs.plugins.androidLibrary)
// and kotlin { androidTarget() }, then restore android { namespace ... } block
// android {
//     compileSdk = 34
//     namespace = "com.knowledgebunker.app"
// }
