# NexaPOS Retail (Android)

A native Android point-of-sale app for retail / general stores. Offline-first:
all data is stored locally in SQLite, so it works with no internet connection.

- **Language / UI:** Kotlin + Jetpack Compose (Material 3)
- **Data:** Room (SQLite), offline-first
- **applicationId:** `com.nexapos.retail`
- **minSdk:** 26 · **target/compileSdk:** 35

## Toolchain (this machine)

The Android toolchain is installed under `D:\Android` (kept off the nearly-full
C: drive). The following user environment variables are already set:

| Variable | Value |
|---|---|
| `JAVA_HOME` | `D:\Android\jdk17` |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | `D:\Android\Sdk` |
| `GRADLE_USER_HOME` | `D:\Android\.gradle` |
| `ANDROID_AVD_HOME` | `D:\Android\.android\avd` |

`local.properties` points Gradle at the SDK (`sdk.dir=D:\\Android\\Sdk`).

## Build

```powershell
$env:JAVA_HOME = "D:\Android\jdk17"
.\gradlew.bat assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk
```

## Run on the emulator

```powershell
# 1. Start the emulator (AVD name: pos_emulator)
D:\Android\Sdk\emulator\emulator.exe -avd pos_emulator -gpu swiftshader_indirect

# 2. Install + launch (adb = D:\Android\Sdk\platform-tools\adb.exe)
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.nexapos.retail/.MainActivity
```

## Project structure

```
app/src/main/java/com/nexapos/retail/
  data/            Room database, entities, DAOs, repositories, demo seeder
    entity/        Category, Product, Sale, SaleItem
    dao/           CategoryDao, ProductDao, SaleDao
    repository/    CatalogRepository, SalesRepository
  di/              AppContainer (manual dependency container)
  ui/
    theme/         Material 3 colors, typography, theme
    sale/          SaleScreen + SaleViewModel (catalog, cart, checkout)
  util/            Money (cents <-> display formatting)
  MainActivity.kt  PosApplication.kt
```

## Money

All amounts are stored as `Long` minor units (cents) to avoid floating-point
errors. Display formatting (currency symbol, currently `Rs`) lives in
`util/Money.kt`.

## Status / roadmap

- [x] Project scaffold, build, run on emulator
- [x] Offline sale flow: catalog → cart → CASH checkout → receipt (persisted)
- [ ] Restyle screens to match the old app (reference images in `reference/`)
- [ ] Product / inventory management screens
- [ ] Payment types, tax, discounts, change calculation
- [ ] Sales history & reports
- [ ] Receipt printing (thermal printer integration)
- [x] Release signing & AAB pipeline (see "Releasing" below)

## Releasing to the Play Store

The release build produces a minified, signed **Android App Bundle** (`.aab`) —
the format Google Play requires for new apps.

### One-time: create your upload keystore
```powershell
D:\Android\jdk17\bin\keytool.exe -genkeypair -v `
  -keystore D:\Android\keystore\nexapos-upload.jks `
  -alias nexapos -keyalg RSA -keysize 2048 -validity 10000
```
Then copy `keystore.properties.template` to `keystore.properties` and fill in the
passwords/alias. **Back up the `.jks` file and its password** — if you lose them
you can never publish an update to this app again. Both files are gitignored.

### Build the signed bundle
```powershell
$env:JAVA_HOME = "D:\Android\jdk17"
.\gradlew.bat bundleRelease
# Output: app\build\outputs\bundle\release\app-release.aab
```
Without `keystore.properties` present, the bundle still builds but is **unsigned**
(handy for CI / verifying the release build).

### Upload
Create the app in the [Google Play Console](https://play.google.com/console),
upload the `.aab` to a testing track, complete the store listing, Data Safety form
and content rating, then promote to production. Bump `versionCode` (and
`versionName`) in `app/build.gradle.kts` for every release.
