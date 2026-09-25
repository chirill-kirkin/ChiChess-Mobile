plugins {
  alias(libs.plugins.jvm.library)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":feature:game:domain"))
  implementation(libs.kotlinx.coroutines.core)
}
