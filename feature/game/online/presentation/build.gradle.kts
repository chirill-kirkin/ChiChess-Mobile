plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.parcelize)
}

android {
  namespace = "com.github.chirillkirkin.chichess.feature.game.online.presentation"
  compileSdk = 37

  defaultConfig {
    minSdk = 29
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
  implementation(project(":feature:game:online:domain"))
  implementation(project(":feature:game:board"))
  implementation(project(":feature:game:domain"))
  implementation(project(":mvu:core"))
  implementation(project(":mvu:android-savedstate"))

  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(project(":feature:game:engine"))
}
