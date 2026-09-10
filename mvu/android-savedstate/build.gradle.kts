plugins {
  alias(libs.plugins.android.library)
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
}

kotlin {
  explicitApi()
  jvmToolchain(17)
}

dependencies {
  api(project(":mvu:core"))
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
}
