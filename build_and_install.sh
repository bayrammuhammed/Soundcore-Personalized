#!/usr/bin/env bash
set -e

PROJECT_DIR="/Users/muhammedbayram/AndroidStudioProjects/SoundcoreSmartX20"
BUILD_DIR="$PROJECT_DIR/build"
SDK_DIR="/Users/muhammedbayram/Library/Android/sdk"
BUILD_TOOLS="$SDK_DIR/build-tools/36.1.0"
PLATFORM_JAR="$SDK_DIR/platforms/android-36/android.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/dex" "$BUILD_DIR/compiled_res"

echo "[1/5] Compiling resources with aapt2..."
"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/app/src/main/res" -o "$BUILD_DIR/compiled_res/res.zip"
"$BUILD_TOOLS/aapt2" link \
  -o "$BUILD_DIR/unsigned.apk" \
  -I "$PLATFORM_JAR" \
  --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
  --java "$BUILD_DIR/gen" \
  "$BUILD_DIR/compiled_res/res.zip"

echo "[2/5] Compiling Java sources with javac..."
javac --release 17 \
  -cp "$PLATFORM_JAR" \
  -d "$BUILD_DIR/obj" \
  "$BUILD_DIR/gen/com/bayram/soundcorex20/R.java" \
  "$PROJECT_DIR/app/src/main/java/com/bayram/soundcorex20/"*.java

echo "[3/5] Converting bytecode to DEX with d8..."
"$BUILD_TOOLS/d8" \
  --lib "$PLATFORM_JAR" \
  --min-api 29 \
  --output "$BUILD_DIR/dex" \
  "$BUILD_DIR/obj/com/bayram/soundcorex20/"*.class

echo "[4/5] Packaging and aligning APK..."
cd "$BUILD_DIR/dex"
zip -u "$BUILD_DIR/unsigned.apk" classes.dex
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

KEYSTORE="$PROJECT_DIR/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

echo "[5/5] Signing APK..."
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$BUILD_DIR/SoundcoreSmartX20.apk" \
  "$BUILD_DIR/aligned.apk"

echo "APK successfully built at: $BUILD_DIR/SoundcoreSmartX20.apk"
ls -lh "$BUILD_DIR/SoundcoreSmartX20.apk"
