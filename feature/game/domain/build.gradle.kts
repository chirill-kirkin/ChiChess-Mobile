plugins {
  alias(libs.plugins.jvm.library)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation(libs.kotlin.test)
}

tasks.test {
  useJUnitPlatform()
}
