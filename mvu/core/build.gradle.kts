plugins {
  alias(libs.plugins.jvm.library)
}

kotlin {
  explicitApi()
  jvmToolchain(17)
}

dependencies {
  api(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
  useJUnitPlatform()
}
