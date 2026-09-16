plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.github.chirillkirkin.chichess.feature.game.board"
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
  implementation(project(":feature:game:domain"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)

  testImplementation(libs.kotlin.test.junit5)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
