import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.ksp)
}

private val serverUrl: String =
  Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
  }.getProperty("chichess.serverUrl")
    ?: error("Missing `chichess.serverUrl` in local.properties (e.g. http://10.0.2.2:8080)")

android {
    namespace = "com.github.chirillkirkin.chichess"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.github.chirillkirkin.chichess"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
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
  // Core modules
  implementation(project(":core:designsystem"))
  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  implementation(project(":core:data:network"))

  // Feature modules
  implementation(project(":feature:game:domain"))
  implementation(project(":feature:game:engine"))
  implementation(project(":feature:game:offline"))
  implementation(project(":feature:game:online"))
  implementation(project(":feature:home:presentation"))

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.activity.compose)
  implementation(libs.dagger.hilt.android)
  ksp(libs.dagger.hilt.compiler)

  // Compose
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
}
