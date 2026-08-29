import java.io.File
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

/**
 * Release signing credentials.
 *
 * Read from `keystore.properties` (git-ignored, next to this file or pointed at by
 * KEYSTORE_PROPERTIES) so the secrets never enter the repo. CI writes the same file
 * from GitHub Secrets before building.
 *
 * When the file is absent — a fresh clone, or a contributor who only builds debug —
 * signing is simply not configured and `assembleRelease` produces an unsigned APK
 * rather than failing the whole build. Debug builds are unaffected either way.
 */
val keystorePropertiesFile: File =
  providers.environmentVariable("KEYSTORE_PROPERTIES").orNull?.let(::File)
    ?: rootProject.file("keystore.properties")

val keystoreProperties =
  Properties().apply {
    if (keystorePropertiesFile.exists()) {
      keystorePropertiesFile.inputStream().use { load(it) }
    }
  }

val hasSigningConfig = keystoreProperties.getProperty("storeFile")?.let { File(it).exists() } == true

android {
    namespace = "com.liktun.japanesehabitlock"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.liktun.japanesehabitlock"
        minSdk = 26
        targetSdk = 36
        // Bumped on every user-visible release. Android refuses to install an APK whose
        // versionCode is not greater than the installed one, so shipping a build with a
        // stale code fails as a confusing "app not installed" rather than an obvious
        // error. Play additionally rejects a re-used code outright.
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
      if (hasSigningConfig) {
        create("release") {
          storeFile = File(keystoreProperties.getProperty("storeFile"))
          storePassword = keystoreProperties.getProperty("storePassword")
          keyAlias = keystoreProperties.getProperty("keyAlias")
          keyPassword = keystoreProperties.getProperty("keyPassword")
          // Both signature schemes: v2 is required for modern Android, v1 keeps
          // sideloading working on older devices within our minSdk 26 range.
          enableV1Signing = true
          enableV2Signing = true
        }
      }
    }

    buildTypes {
        release {
            // R8 on: it strips unused Compose and DataStore code, which is most of the
            // APK. Shrinking resources too since the six themes ship a lot of unused
            // Material defaults.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
              signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Persistence: shared between the app UI and (later) the Accessibility Service
  implementation(libs.androidx.datastore.preferences)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
