import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.application")
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64(), iosX64())
    iosTargets.forEach { target -> target.binaries.framework { baseName = "ComposeApp"; isStatic = true } }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("io.coil-kt.coil3:coil-compose:3.1.0")
            implementation("io.coil-kt.coil3:coil-network-ktor2:3.1.0")
            implementation("io.github.alexzhirkevich:qrose:1.0.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            implementation("io.ktor:ktor-client-mock:2.3.12")
        }
        androidMain.dependencies { implementation("io.ktor:ktor-client-okhttp:2.3.12"); implementation("androidx.activity:activity-compose:1.10.1") }
        val desktopMain by getting { dependencies { implementation(compose.desktop.currentOs); implementation("io.ktor:ktor-client-cio:2.3.12"); implementation("org.openjfx:javafx-media:21.0.2:linux"); implementation("org.openjfx:javafx-graphics:21.0.2:linux"); implementation("org.openjfx:javafx-base:21.0.2:linux") } }
        iosMain.dependencies { implementation("io.ktor:ktor-client-darwin:2.3.12") }
    }
}

android { namespace = "dev.vespera.player"; compileSdk = 35
    defaultConfig { applicationId = "dev.vespera.player"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "0.2.0-dev" }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    signingConfigs { getByName("debug") }
    buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("debug"); isMinifyEnabled = false } }
}

compose.desktop { application { mainClass = "dev.vespera.player.MainKt"; nativeDistributions { targetFormats(TargetFormat.Deb, TargetFormat.Rpm); packageName = "Vespera"; packageVersion = "0.2.0" } } }
