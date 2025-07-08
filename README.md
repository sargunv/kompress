[![Maven Central Version](https://img.shields.io/maven-central/v/kompress/kompress?label=Maven)](https://central.sonatype.com/namespace/kompress)
[![License](https://img.shields.io/github/license/sargunv/kompress?label=License)](https://github.com/sargunv/kompress/blob/main/LICENSE)
[![Kotlin Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fsargunv%2Fkompress%2Frefs%2Fheads%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.gradle-kotlin&prefix=v&logo=kotlin&label=Kotlin)](./gradle/libs.versions.toml)
[![Documentation](https://img.shields.io/badge/Documentation-blue?logo=MaterialForMkDocs&logoColor=white)](https://sargunv.github.io/kompress/)
[![API Reference](https://img.shields.io/badge/API_Reference-blue?logo=Kotlin&logoColor=white)](https://sargunv.github.io/kompress/api/)

# Kompress

Kompress is a WIP [Kotlin Multiplatform] library for compression and
decompression of data. It will contain:

- a pure Kotlin port of [zlib] `deflate` and `inflate`.
- platform-specific zlib wrappers as alternative implementations.
- a [kotlinx-io] friendly API for streaming compression / decompression.
- a zip archive reader and writer.

**Status**

| API                               | Compress | Decompress |
|-----------------------------------|----------|------------|
| Common: pure Kotlin zlib port     | 🚫       | 🚧         |
| JVM: `java.util.zip` wrapper      | ✅        | ✅          |
| Native: `platform.zlib` wrapper   | ✅        | ✅          |
| JS: `pako` or `node:zlib` wrapper | 🚫       | 🚫         |
| Common: Zip reader                | 🚫       | 🚫         |
| Common: Zip writer                | 🚫       | 🚫         |

## Usage

- [Documentation](https://sargunv.github.io/kompress/) (TODO)
- [API Reference](https://sargunv.github.io/kompress/api/)

[Kotlin Multiplatform]: https://kotlinlang.org/docs/multiplatform.html

[zlib]: https://www.zlib.net/

[kotlinx-io]: https://github.com/Kotlin/kotlinx-io
