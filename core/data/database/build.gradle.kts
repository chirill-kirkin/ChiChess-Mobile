plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.github.chirillkirkin.chichess.core.data.database"
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
  jvmToolchain(17)
}
