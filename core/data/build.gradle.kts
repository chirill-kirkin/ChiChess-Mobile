plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.github.chirillkirkin.chichess.core.data"
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
  implementation(project(":core:domain"))
  implementation(project(":core:data:network"))

  implementation(libs.ktor.client.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.androidx.datastore)

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.ktor.client.mock)
}
