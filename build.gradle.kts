@file:OptIn(ExperimentalWasmDsl::class)

import fr.brouillard.oss.jgitver.Strategies
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsTask

plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mkdocs)
  alias(libs.plugins.jgitver)
  alias(libs.plugins.kover)
}

group = "dev.sargunv.kompress"

jgitver {
  strategy(Strategies.MAVEN)
  nonQualifierBranches("main")
}

dokka { moduleName = "Kompress API Reference" }

mkdocs {
  sourcesDir = "docs"
  strict = true
  publish {
    docPath = null // single version site
  }
}

tasks.withType<MkdocsTask>().configureEach {
  val releaseVersion = ext["base_tag"].toString().replace("v", "")
  val snapshotVersion = "${ext["next_patch_version"]}-SNAPSHOT"
  extras.set(mapOf("release_version" to releaseVersion, "snapshot_version" to snapshotVersion))
}

tasks.register("generateDocs") {
  dependsOn("dokkaGenerate", "mkdocsBuild")
  doLast {
    copy {
      from(layout.buildDirectory.dir("mkdocs"))
      into(layout.buildDirectory.dir("docs"))
    }
    copy {
      from(layout.buildDirectory.dir("dokka/html"))
      into(layout.buildDirectory.dir("docs/api"))
    }
  }
}

dependencies {
  dokka(project(":lib:kompress-core:"))
  dokka(project(":lib:kompress-ktxio:"))

  kover(project(":lib:kompress-core:"))
  kover(project(":lib:kompress-ktxio:"))
}

spotless {
  kotlinGradle {
    target("*.gradle.kts", "lib/*/*.gradle.kts")
    ktfmt().googleStyle()
  }
  kotlin {
    target("lib/*/src/**/*.kt")
    ktfmt().googleStyle()
  }
  format("markdown") {
    target("*.md", "lib/*/*.md", "docs/**/*.md")
    prettier(libs.versions.tool.prettier.get()).config(mapOf("proseWrap" to "always"))
  }
  yaml {
    target(".github/**/*.yml")
    prettier(libs.versions.tool.prettier.get())
  }
}

tasks.register("installGitHooks") {
  doLast {
    copy {
      from("${rootProject.projectDir}/scripts/pre-commit")
      into("${rootProject.projectDir}/.git/hooks")
    }
  }
}

tasks.named("clean") { doLast { delete("${rootProject.projectDir}/.git/hooks/pre-commit") } }
