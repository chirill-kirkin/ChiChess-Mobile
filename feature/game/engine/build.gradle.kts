plugins {
  alias(libs.plugins.jvm.library)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":feature:game:domain"))

  implementation(libs.chesslib)
  implementation(libs.javax.inject)

  testImplementation(libs.kotlin.test)
}

tasks.test {
  useJUnitPlatform()
}
