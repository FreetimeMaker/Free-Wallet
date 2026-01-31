#!/bin/bash
set -e

### CONFIG ###
APP_ID="com.freetime.wallet"
TAG="v1.0.0"   # <-- HIER deine Version eintragen
KEYSTORE="$HOME/AndroidStudioProjects/Freetime-Wallet/Freetime-Wallet-KeyStore.jks"
KEY_ALIAS="alle"
KEY_PASS="KKKKKK"
OUT_APK="Freetime-Wallet-$TAG.apk"
################

echo "==> Erstelle Tag $TAG"
git tag "$TAG"

echo "==> Pushe Tag $TAG auf GitHub"
git push origin "$TAG"

echo "==> Hole Tags von GitHub"
git fetch --tags

echo "==> Checkout des Release-Tags: $TAG"
git checkout "$TAG"

echo "==> Sauberer Build"
./gradlew clean assembleRelease

UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

if [ ! -f "$UNSIGNED_APK" ]; then
    echo "FEHLER: Unsigned APK nicht gefunden!"
    exit 1
fi

echo "==> Signiere APK"
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  --out "$OUT_APK" \
  "$UNSIGNED_APK"

echo "==> Prüfe Signatur"
apksigner verify --verbose "$OUT_APK"

echo "==> Fertig!"
echo "Signierte APK: $OUT_APK"
