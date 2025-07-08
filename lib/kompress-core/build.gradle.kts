@file:OptIn(ExperimentalWasmDsl::class)

import fr.brouillard.oss.jgitver.Strategies
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.jgitver)
  alias(libs.plugins.kover)
  id("maven-publish")
}

group = "dev.sargunv.kompress"

jgitver {
  strategy(Strategies.MAVEN)
  nonQualifierBranches("main")
}

kotlin {
  jvmToolchain(21)
  explicitApiWarning()
  compilerOptions {
    allWarningsAsErrors = true
    optIn = listOf("kotlin.ExperimentalUnsignedTypes", "kotlin.time.ExperimentalTime")
    freeCompilerArgs.addAll(
      "-Xexpect-actual-classes",
      "-Xcontext-sensitive-resolution",
      "-Xconsistent-data-class-copy-visibility",
    )
  }

  jvm()

  js(IR) {
    browser()
    nodejs()
    compilerOptions { useEsClasses = true }
  }

  wasmJs {
    browser()
    nodejs()
    d8()
  }

  macosX64()
  macosArm64()
  iosSimulatorArm64()
  iosX64()
  iosArm64()
  linuxX64()
  linuxArm64()
  watchosSimulatorArm64()
  watchosX64()
  watchosArm32()
  watchosArm64()
  tvosSimulatorArm64()
  tvosX64()
  tvosArm64()
  androidNativeArm32()
  androidNativeArm64()
  androidNativeX86()
  androidNativeX64()
  mingwX64()
  watchosDeviceArm64()

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      implementation(kotlin("stdlib"))
      implementation(libs.kotlinx.io.bytestring)
    }

    create("platformZlibMain") {
      dependsOn(commonMain.get())
      jvmMain.get().dependsOn(this)
      nativeMain.get().dependsOn(this)
    }

    create("platformZlibTest") {
      dependsOn(commonTest.get())
      jvmTest.get().dependsOn(this)
      nativeTest.get().dependsOn(this)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.io.core)
    }
  }
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      setUrl("https://maven.pkg.github.com/sargunv/kompress")
      credentials(PasswordCredentials::class)
    }
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
  pom {
    name = "Kompress"
    description = "A Kotlin Multiplatform library for compression and decompression of data."
    url = "https://sargunv.github.io/kompress/"
    licenses {
      license {
        name.set("MIT")
        url.set("https://opensource.org/license/mit")
        distribution.set("repo")
      }
      license {
        name.set("Zlib")
        url.set("https://opensource.org/license/zlib")
        distribution.set("repo")
      }
    }
    developers {
      developer {
        id.set("sargunv")
        name.set("Sargun Vohra")
        url.set("https://github.com/sargunv")
      }
    }
    scm {
      url.set("https://github.com/sargunv/kompress")
      connection.set("scm:git:git://github.com/sargunv/kompress.git")
      developerConnection.set("scm:git:ssh://git@github.com/sargunv/kompress.git")
    }
  }
}

dokka {
  dokkaSourceSets {
    configureEach {
      includes.from("MODULE.md")
      sourceLink {
        remoteUrl("https://github.com/sargunv/kompress/tree/${project.ext["base_tag"]}/")
        localDirectory.set(rootDir)
      }
      externalDocumentationLinks {
        create("kotlinx-io") { url("https://kotlinlang.org/api/kotlinx-io/") }
      }
    }
  }
}
