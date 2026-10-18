plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.github.chirillkirkin.chichess.feature.game.offline.data"
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
  implementation(project(":feature:game:domain"))
  implementation(project(":feature:game:offline:domain"))
  implementation(project(":core:data:database"))

  testImplementation(libs.kotlin.test.junit5)
}
