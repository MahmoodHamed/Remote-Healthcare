#!/usr/bin/env bash
# Build helpers — run from anywhere; paths are absolute to the repo root.
set -euo pipefail

ROOT="/var/www/Remote-Healthcare"
BACKEND="$ROOT/backend"
DOCKER_DIR="$ROOT/docker"

usage() {
  cat <<'EOF'
Usage: scripts/build.sh <target>

  backend       Build API image via Docker (no local dotnet SDK required)
  migrate       Apply EF migrations (requires postgres; uses Docker SDK image)
  watchapp      Build Samsung watch APK (requires JDK 17+)
  android       Build mobile APK (requires gradlew + JDK — see README below)
  web           Build React web app (npm run build)

Examples (from any directory):
  bash /var/www/Remote-Healthcare/scripts/build.sh backend
  bash /var/www/Remote-Healthcare/scripts/build.sh migrate
EOF
}

build_backend() {
  docker build -f "$BACKEND/src/RPM.API/Dockerfile" -t rpm-api:latest "$BACKEND"
  echo "OK: image rpm-api:latest"
}

run_migrate() {
  # Default connection matches docker-compose postgres service on host port 5432
  CONN="${RPM_DB_CONNECTION:-Host=127.0.0.1;Port=5432;Database=rpm_db;Username=rpm_user;Password=rpm_password}"
  docker run --rm \
    -v "$BACKEND:/src" -w /src \
    --network host \
    mcr.microsoft.com/dotnet/sdk:10.0 \
    bash -c "dotnet tool install --global dotnet-ef 2>/dev/null || true; \
      export PATH=\"\$PATH:/root/.dotnet/tools\"; \
      dotnet ef database update --project src/RPM.Infrastructure --startup-project src/RPM.API --connection \"$CONN\""
  echo "OK: database updated"
}

build_web() {
  (cd "$ROOT/web-app" && npm run build)
  echo "OK: web-app/dist"
}

build_watchapp() {
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java not installed. Install JDK 17, e.g.: apt install openjdk-17-jdk"
    exit 1
  fi
  local sdk="${ANDROID_HOME:-/opt/android-sdk}"
  if [[ ! -d "$sdk/platforms" ]]; then
    echo "ERROR: Android SDK not found at $sdk"
    echo "       Install once: bash $ROOT/scripts/install-android-sdk.sh"
    exit 1
  fi
  mkdir -p "$ROOT/watchapp"
  echo "sdk.dir=$sdk" > "$ROOT/watchapp/local.properties"
  export ANDROID_HOME="$sdk" JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
  (cd "$ROOT/watchapp" && ./gradlew :app:assembleDebug)
  echo "OK: watchapp/app/build/outputs/apk/debug/"
}

build_android() {
  if [[ ! -x "$ROOT/android/gradlew" ]]; then
    echo "ERROR: android/gradlew missing. Open the project in Android Studio once to generate the wrapper,"
    echo "       or copy gradlew from watchapp/ into android/."
    exit 1
  fi
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java not installed. Install JDK 17, e.g.: apt install openjdk-17-jdk"
    exit 1
  fi
  (cd "$ROOT/android" && ./gradlew :app:assembleDebug)
  echo "OK: android/app/build/outputs/apk/debug/"
}

case "${1:-}" in
  backend)  build_backend ;;
  migrate)  run_migrate ;;
  web)      build_web ;;
  watchapp) build_watchapp ;;
  android)  build_android ;;
  *)        usage; exit 1 ;;
esac
