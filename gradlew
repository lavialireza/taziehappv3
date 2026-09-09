#!/bin/sh
# Gradle Wrapper bootstrap for TaziehApp.
# It downloads the pinned Gradle distribution from gradle-wrapper.properties,
# then delegates all arguments to that exact Gradle version.
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
DIST_URL="https://services.gradle.org/distributions/gradle-8.6-bin.zip"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/tazieh-gradle-8.6"
GRADLE_HOME="$CACHE_DIR/gradle-8.6"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  ZIP="$CACHE_DIR/gradle-8.6-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --connect-timeout 15 -o "$ZIP" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "$DIST_URL"
    else
      echo "curl یا wget برای دریافت Gradle لازم است." >&2
      exit 1
    fi
  fi
  rm -rf "$CACHE_DIR/gradle-8.6.tmp"
  mkdir -p "$CACHE_DIR/gradle-8.6.tmp"
  unzip -q "$ZIP" -d "$CACHE_DIR/gradle-8.6.tmp"
  mv "$CACHE_DIR/gradle-8.6.tmp/gradle-8.6" "$GRADLE_HOME"
  rm -rf "$CACHE_DIR/gradle-8.6.tmp"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
