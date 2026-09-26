# Changelog

All notable changes to this fork of ODK Briefcase are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [semantic versioning](https://semver.org/) once a release is tagged
(`vX.Y.Z`, produced by `git describe` and written into `BuildConfig.VERSION`
and the JAR file name).

This repository ([GeekLord/odk-briefcase](https://github.com/GeekLord/odk-briefcase)),
maintained by **Shobhit Kumar Prabhakar**, is the current ODK Briefcase codebase.
The upstream project [getodk/briefcase](https://github.com/getodk/briefcase) is archived.
Its last release was [v1.18.0](https://github.com/getodk/briefcase/releases/tag/v1.18.0)
(4 November 2020). Everything below is work done in this fork on top of that tree.

## [Unreleased]

## [2.0.1] - 2026-09-26
### Fixed
- Point Briefcase version checks to the active GeekLord/odk-briefcase fork to stop showing update messages that direct users back to the archived parent repository.

## [2.0.0] - 2026-09-22

First release of this fork. Requires **Java 17 or newer** (v1.18.0 ran on Java 8).

Modernization of the build, toolchain, and dependencies so the project compiles,
tests, and packages on a current JDK. Application behavior (pull, push, export,
settings, CLI) is unchanged aside from the compatibility fixes listed under
**Fixed**, which keep existing features working against the upgraded libraries.

### Changed

- **Java 17.** Sources compile with `javac --release 17`. Building and running
  the JAR requires JDK 17 or newer. Java 8 and Java 11 are no longer supported,
  and the old unlimited-strength JCE policy instructions no longer apply.
- **Gradle 5.4.1 → 9.7.1** via the wrapper (`gradle/wrapper`). `build.gradle`
  was rewritten for current Gradle:
  - `compile` / `testCompile` / `runtime` replaced by `implementation` and
    `testImplementation`.
  - `mainClassName` replaced by `application.mainClass`.
  - Version string comes from `providers.exec` running
    `git describe --tags --dirty --always` (the removed `Project.exec` API is
    not used).
  - JaCoCo reports use the `required` property instead of `enabled`.
  - Archive names use `archiveFileName`; copy paths use `layout.buildDirectory`.
  - `settings.gradle` sets the project name to `odk-briefcase`.
  - Test workers pass `--add-opens` for `java.desktop` and `java.base` so
    AssertJ Swing can reflect into Swing on JDK 17+.
  - The build reports no Gradle deprecation warnings under `--warning-mode all`.
- **`BuildConfig` generation** no longer uses the unmaintained
  `de.fuerstenau.buildconfig` plugin. The `generateBuildConfig` task writes
  `org.opendatakit.briefcase.buildconfig.BuildConfig` with the same fields:
  `VERSION`, `NAME`, `GOOGLE_TRACKING_ID`, `SENTRY_ENABLED`, `SENTRY_DSN`.
  `gradle.properties` keys `sentry.enabled`, `sentry.dsn`, and
  `googleAnalytics.trackingId` still override the defaults.
- **Checkstyle 8.22 → 12.3.1**, still configured from
  `config/checkstyle/checkstyle.xml`. Checkstyle 13 and later need Java 21,
  so 12.3.1 is the newest release that runs on the Java 17 build.

### Dependencies

| Dependency | Was | Now |
| --- | --- | --- |
| JavaRosa | 2.17.2 | 6.0.0 |
| Bouncy Castle | `bcprov-jdk16` 1.46 | `bcprov-jdk18on` and `bcpkix-jdk18on` 1.86 |
| Apache HttpClient, httpmime, fluent-hc | 4.5.5 | 4.5.14 |
| Commons IO | 2.6 | 2.22.0 |
| Commons CLI | 1.4 | 1.11.0 |
| HSQLDB | 2.4.0 | 2.7.4 |
| LGoodDatePicker | 10.3.1 | 11.2.1 |
| geojson-jackson | 1.8 | 1.14 |
| Jackson (core, databind, annotations) | transitive | 2.22.3 (BOM) |
| SLF4J (`slf4j-api`, `jcl-over-slf4j`) | 1.7.25 | 2.0.20 |
| Logback Classic | 1.2.3 | 1.6.3 |
| Sentry (`sentry-logback`) | 1.6.8 | 8.57.0 |
| JUnit | 4.12 | 4.13.2 |
| Hamcrest | 1.3 (`hamcrest-library`) | 3.0 (`hamcrest`) |
| AssertJ Swing | 3.8.0 | 3.17.1 |
| Moco | 0.12.0 | 1.6.1 |
| EventBus | 1.4 | 1.4 (unchanged; final release) |
| kXML2 | 2.3.0 | 2.3.0 (unchanged) |
| google-analytics-java | 2.0.0 | 2.0.0 (unchanged; final release) |
| SmallSQL | bundled `lib/smallsql-0.21.jar` | unchanged |

Also excluded JavaRosa's incorrectly published `hamcrest-all` test jar from the
runtime classpath. geojson-jackson 3.0 was not adopted because it requires
Jackson 3 (`tools.jackson`), which conflicts with the Jackson 2 line used by
JavaRosa and the rest of the application.

The packaged JAR is still built with the Eclipse jar-in-jar loader. Bouncy
Castle jars are signed, so they are nested rather than unpacked into a fat JAR.

### Fixed

- **PEM private keys (Bouncy Castle).** `PEMReader` was removed years ago.
  `ExportConfiguration` now reads keys with `PEMParser` and
  `JcaPEMKeyConverter`. Both PKCS#1 (`BEGIN RSA PRIVATE KEY`, via `PEMKeyPair`)
  and PKCS#8 (`BEGIN PRIVATE KEY`, via `PrivateKeyInfo`) files are accepted.
- **JavaRosa 6 form parsing.** `XFormParser.parse()` throws the checked
  `XFormParser.ParseException`. `FormDefinition` and
  `BaseFormParserForJavaRosa` catch it. `FormDef.populateDynamicChoices` and
  `ItemsetBinding.getChoices()` with no arguments are gone; itemset choices are
  resolved with `ItemsetBinding.getChoices(formDef, reference)` while the form
  is loaded and stored on the question, which is what split-select-multiple
  export reads.
- **Concurrent XForm parsing.** JavaRosa 6 allows only one `XFormParser.parse()`
  at a time and fails the others immediately. Briefcase parses forms from the
  UI thread and from parallel pull, push, and export jobs.
  `org.opendatakit.briefcase.util.XFormParsing` serializes those calls so a
  second parse waits instead of throwing.
- **Sentry 8 crash reporting.** The 1.x client (`Sentry.init(String)`,
  `SentryClient.sendException`, `addShouldSendEventCallback`) is replaced by
  `Sentry.init(options)`, `Sentry.captureException`, and a `beforeSend`
  callback. Reports are still sent only when `sentry.enabled=true`, and they
  are dropped when the user has turned tracking consent off. `Sentry.flush` is
  called before `System.exit` so the async event can leave.

### CI

- Removed `.circleci/config.yml` (CircleCI image `circleci/openjdk:11-jdk`).
- Added `.github/workflows/build.yml`. It checks out full git history (so
  `git describe` works), sets up Temurin 17, copies the logback example configs,
  and runs `xvfb-run ./gradlew check jacocoTestReport jar`. Test reports and
  the JAR are uploaded as workflow artifacts. The workflow runs on pushes to
  `master`, on pull requests, and on manual dispatch.

### Documentation

- README rewritten for this fork: maintainer, archived upstream, Java 17
  requirement, Gradle wrapper workflow, dependency table, and GitHub Actions
  downloads.
- `docs/how-to-release.md` step that required the Java.net OpenJDK 11.0.2 build
  now requires any JDK 17 or newer.

### Verified

On JDK 21 with Gradle 9.7.1:

- `./gradlew clean check jacocoTestReport jar explodedJar` under Xvfb:
  408 tests, 0 failures, 8 skipped (same counts as the Gradle 5.4.1 / JDK 11
  baseline), checkstyle clean.
- The suite was run three more times after the `XFormParsing` lock was added;
  all three passed.
- `java -jar build/libs/ODK-Briefcase-*.jar --help` prints the CLI operations.
- The Swing window starts from the packaged JAR. On a first launch only
  Settings is enabled until a storage directory is chosen; after that, Pull,
  Push, Export, and Settings all open.

[Unreleased]: https://github.com/GeekLord/odk-briefcase/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/GeekLord/odk-briefcase/releases/tag/v2.0.1
[2.0.0]: https://github.com/GeekLord/odk-briefcase/releases/tag/v2.0.0
