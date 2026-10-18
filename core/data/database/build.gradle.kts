plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ksp)
  alias(libs.plugins.androidx.room3)
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

room3 {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  api(libs.androidx.room3.runtime)
  ksp(libs.androidx.room3.compiler)
}
