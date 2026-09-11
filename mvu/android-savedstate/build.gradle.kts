plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.parcelize)
}

android {
  namespace = "com.github.chirillkirkin.mvu.savedstate"
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
  explicitApi()
  jvmToolchain(17)
}

dependencies {
  api(project(":mvu:core"))
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.kotlinx.coroutines.test)
}
