pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RealtimeChat"
include(":app")
include(":core:model")
include(":core:crypto")
include(":core:network")
include(":core:network:firebase")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":feature:auth")
include(":feature:chat")
include(":feature:group")
include(":feature:conversations")
