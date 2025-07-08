@file:Suppress("UnstableApiUsage")

rootProject.name = "kompress"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement { repositories { mavenCentral() } }

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0") }

include(":", ":lib", ":lib:kompress-core", ":lib:kompress-ktxio")
