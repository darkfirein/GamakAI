# Gamak AI — Release Build & Signing Guide

This guide describes how to configure, sign, and build a production-ready Release APK or Android App Bundle (AAB) for Gamak AI.

---

## 1. Security Guidelines for Release

- **Never commit `.keystore` or `.jks` files** to your source control or public GitHub repository.
- **Never commit production API keys** to `.env` or `local.properties`.
- In production, route sensitive Gemini API calls through an authenticated backend proxy to protect API secrets.

---

## 2. Generating a Release Keystore

Run the following command in your terminal to create a standard 2048-bit RSA release signing key:

```bash
keytool -genkey -v \
  -keystore gamak-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias gamak_release_alias
```

You will be prompted to create passwords for the keystore and the key alias.

---

## 3. Configuring Gradle for Release Signing

You can pass signing credentials via environment variables without hardcoding them in `app/build.gradle.kts`:

```bash
export RELEASE_KEYSTORE_PATH="/path/to/gamak-release-key.jks"
export RELEASE_KEYSTORE_PASSWORD="your-keystore-password"
export RELEASE_KEY_ALIAS="gamak_release_alias"
export RELEASE_KEY_PASSWORD="your-key-password"
```

Or configure `local.properties` (which is excluded in `.gitignore`):

```properties
RELEASE_KEYSTORE_PATH=/path/to/gamak-release-key.jks
RELEASE_KEYSTORE_PASSWORD=your-keystore-password
RELEASE_KEY_ALIAS=gamak_release_alias
RELEASE_KEY_PASSWORD=your-key-password
```

---

## 4. Building the Production Release Artifacts

### A. Build Release APK
```bash
./gradlew :app:assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/app-release.apk` (or unsigned at `app-release-unsigned.apk` if signing is unset)

### B. Build Production Android App Bundle (AAB) for Google Play
```bash
./gradlew :app:bundleRelease
```
The output AAB is generated at:
`app/build/outputs/bundle/release/app-release.aab`

---

## 5. ProGuard / R8 Optimization

Gamak AI includes customized ProGuard / R8 rules in `app/proguard-rules.pro` optimizing:
- Room Database DAOs and Entities
- Kotlinx Serialization serializers
- Gemini AI client communication
- Coroutine exception stack-trace line numbering
