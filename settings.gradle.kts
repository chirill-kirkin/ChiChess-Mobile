pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ChiChess"
include(":app")
include(":mvu:core")
include(":mvu:android-savedstate")
include(":core:domain")
include(":core:data")
include(":core:data:network")
include(":core:data:database")
include(":core:designsystem")
include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")
include(":feature:game:domain")
include(":feature:game:engine")
include(":feature:game:board")
include(":feature:game:offline")
include(":feature:game:online:domain")
include(":feature:game:online:data")
include(":feature:game:online:presentation")
