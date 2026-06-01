# NexaPOS Retail — Project Instructions

Native Android retail POS (Kotlin + Jetpack Compose), fully offline and encrypted.
Sold as a one-time purchase. Sample tenant: QUINCAILLERIE RB TRADING (Curepipe, Mauritius).

## Stack
- **Language/UI:** Kotlin, Jetpack Compose ("Workshop Precision" theme; Hanken / JetBrains Mono / Fraunces fonts)
- **Architecture:** MVVM + repository interfaces (`domain/repository`) with Room-backed implementations (`data/repository`)
- **Persistence:** Room/SQLite, encrypted at rest with **SQLCipher (AES-256)**; key in Android Keystore-backed EncryptedSharedPreferences
- **Auth:** local staff PIN (PBKDF2-HMAC-SHA256, 120k iterations); no cloud
- **Backup:** encrypted DB copy to a user-chosen folder via SAF (no cloud account)
- **DI:** manual `di/AppContainer.kt`
- `applicationId com.nexapos.retail`, minSdk 26 / target 35

## Conventions
- **Money is stored as integer cents.** Use `util/Money.kt`; currency is `Rs`. VAT is 15% **inclusive**.
- Sales persist atomically: decrement stock + assign sequential invoice numbers (`S-00001`, …) in one transaction.
- Keep files under ~500 lines. Validate input at system boundaries.
- ALWAYS read a file before editing it. Prefer editing existing files over creating new ones.
- NEVER commit secrets: `keystore.properties`, `*.jks`, `local.properties` are gitignored — keep it that way.

## Build & test
Set `JAVA_HOME` to JDK 17 first (see `README.md`). Run `ktlintFormat` in its **own** Gradle invocation before `ktlintCheck`.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest detekt ktlintCheck --continue
```

Emulators: `pos_emulator` (phone), `pos_tablet` (tablet). CI: `.github/workflows/ci.yml`.

## Docs
- `docs/ARCHITECTURE.md` — module/layer design
- `docs/PHASE5_REPORT.md` — current accurate feature state (offline backend)
- `docs/SCREENS.md` — full screen list & phases
- `reference/handoff/` — design source of truth
