// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.jvm.library) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.dagger.hilt) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.detekt)
}

allprojects {
  apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

  detekt {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
    // Release has no build-type-specific sources, so analysing it only duplicates the debug findings.
    ignoredBuildTypes = listOf("release")
  }

  dependencies {
    detektPlugins(rootProject.libs.detekt.rules.ktlint.wrapper)
    detektPlugins(rootProject.libs.compose.rules.detekt)
  }

  // Detekt runs manually through detektMain and detektTest, so the plugin's check dependency is removed.
  tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
    setDependsOn(dependsOn.filterNot { (it as? TaskProvider<*>)?.name == "detekt" })
  }
}
