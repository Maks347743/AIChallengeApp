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
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AIChallengeApp"
include(":app")
include(":core:database")
include(":feature:chat-settings")
include(":feature:chat")
include(":feature:chat-list")
include(":feature:user-preferences")
include(":core:mcp")
include(":core:periodic-task")
include(":github-mcp-server")
include(":rag-server")
include(":support-mcp-server")
