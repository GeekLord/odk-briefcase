#!/usr/bin/env bash
# Cloud Agent install script for ODK Briefcase.
# Idempotent: safe to run repeatedly against cached/partial state.
set -euo pipefail

cd "$(dirname "$0")/.."

# ODK Briefcase targets Java 8 and its CI builds with JDK 11. The base image
# makes JDK 11 the default `java`/`javac`, so Gradle 5.4.1 (via the wrapper)
# picks it up automatically. Export it explicitly here so the install step is
# robust even if the default alternative changes.
if [ -d /usr/lib/jvm/java-11-openjdk-amd64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
fi

# Logback config files are gitignored, so recreate them from the checked-in
# examples after each checkout. Briefcase loads res/logback.xml at runtime and
# test/resources/logback-test.xml while running tests.
[ -f res/logback.xml ] || cp res/logback.xml.example res/logback.xml
[ -f test/resources/logback-test.xml ] || cp test/resources/logback-test.xml.example test/resources/logback-test.xml

# Warm the Gradle cache and generate BuildConfig so the tree is ready to run,
# test, and package. --no-daemon keeps install self-contained and terminating.
./gradlew --no-daemon compileJava compileTestJava
