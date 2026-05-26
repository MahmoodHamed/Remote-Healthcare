#!/usr/bin/env bash
# Install Android SDK command-line tools + API 34 (for watchapp/android builds on Linux).
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
TMP_ZIP="/tmp/android-cmdline-tools.zip"

echo "Installing Android SDK to $ANDROID_HOME ..."

mkdir -p "$ANDROID_HOME/cmdline-tools"
if [[ ! -f "$TMP_ZIP" ]]; then
  curl -fsSL -o "$TMP_ZIP" "$ZIP_URL"
fi

rm -rf /tmp/cmdline-tools-extract
python3 -c "import zipfile; zipfile.ZipFile('$TMP_ZIP').extractall('/tmp/cmdline-tools-extract')"
rm -rf "$ANDROID_HOME/cmdline-tools/latest"
mv /tmp/cmdline-tools-extract/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
chmod +x "$ANDROID_HOME/cmdline-tools/latest/bin/"*

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Done. Set in your shell profile:"
echo "  export ANDROID_HOME=$ANDROID_HOME"
echo "  export PATH=\$ANDROID_HOME/platform-tools:\$PATH"
