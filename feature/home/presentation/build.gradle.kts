plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.github.chirillkirkin.chichess.feature.home.presentation"
  compileSdk = 37

  defaultConfig {
    minSdk = 29
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":core:designsystem"))
  implementation(project(":mvu:core"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.dagger.hilt.android)
  ksp(libs.dagger.hilt.compiler)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
