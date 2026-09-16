plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.github.chirillkirkin.chichess.feature.game.offline"
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

  testOptions {
    unitTests.all {
      it.useJUnitPlatform()
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":core:designsystem"))
  implementation(project(":feature:game:board"))
  implementation(project(":feature:game:domain"))
  implementation(project(":mvu:core"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.dagger.hilt.android)
  ksp(libs.dagger.hilt.compiler)

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(project(":feature:game:engine"))

  debugImplementation(libs.androidx.compose.ui.tooling)
}
