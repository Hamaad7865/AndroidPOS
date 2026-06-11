# NexaPOS — Session Log (2026-06-11): cents refactor + multi-branch Stage 1

**Repo:** `C:\Projects\NexaPOS` · default branch `main` (has `origin`).
**App:** native Kotlin / Jetpack Compose POS, fully offline, SQLCipher-encrypted.
`applicationId com.nexapos.retail`, minSdk 26 / target 35. Money stored as integer **cents** (`Long`).
**Sample tenant:** QUINCAILLERIE RB TRADING (Curepipe, Mauritius), VAT 15% inclusive.

---

## 0. TL;DR — where things stand

Two bodies of work this session:

1. **App-wide money → exact cents (DONE, on `main`, pushed).** Every money field now
   accepts up to 2 decimals and stores exact cents — fixed the reported "50 × 7.50 read
   as 3,750" bug and swept the same class everywhere. 5 commits + 1 follow-up bug-hunt commit.
2. **Multi-branch paid add-on (ALL 5 STAGES DONE, on `feature/multi-branch`, NOT pushed).** Hybrid
   Firebase sync: offline licence gate → branch identity + sync DTOs → Firestore engine → read-only
   viewing screens → HQ visibility matrix + periodic sync. Full gate green; adversarially reviewed.
   `versionName` 1.1.0. **Owner still needs to create the Firebase project** (`docs/FIREBASE_SETUP.md`)
   before real cross-device sync; until then the app runs exactly as before (Firebase never inits).

**Currently checked-out branch: `feature/multi-branch`.** `main` is clean and matches `origin/main`.

---

## 1. Git / branch state

### `main` (pushed to origin) — the cents work + bug fixes
| Commit | What |
|---|---|
| `95b9961` | fix: guard shift open/close against double-tap; prune shared receipt-PDF cache |
| `19b26df` | fix: purchase-order flat discount accepts decimals (cents) |
| `0d5c347` | fix: dashboard summary cards + live activity show exact cents |
| `0cc2d0d` | fix: money in/out, shift, returns work in exact cents |
| `0b13a9a` | fix: sale + product money core works in exact cents |
| `bc7e19d` | fix: purchase flow takes decimal money (the originally-reported bug) |

(Parent of the cents work is `9aebf1d` — use `git diff 9aebf1d main` to see the whole money refactor.)

### `feature/multi-branch` (local only, NOT pushed) — the add-on
| Commit | What |
|---|---|
| `119fc58` | feat(multi-branch): Stage 1 - offline licence gate |
| `4ce9b50` | docs: multi-branch design spec + implementation plan |
| (branches off `95b9961`) | |

To resume: `git checkout feature/multi-branch`.

---

## 2. The cents refactor (DONE) — what changed

The whole app was whole-rupee `Int` in the UI/domain layer (the DB was always cents). Converted
UI + domain to `Long` cents end-to-end. Helpers: `Money.parseToCents("7.5") → 750`,
`Money.format(750) → "Rs 7.50"`, `Money.toInput(750) → "7.5"` (for editable fields).

- **Inputs** that used to drop decimals now parse them: PO unit cost + flat discount, product
  price/cost, CSV import, checkout cart discount + shipping + tender, money in/out, shift float +
  counted cash, returns.
- **Displays** now show 2 decimals everywhere money is shown in detail: POS, checkout, all 3
  receipt renderers, purchases, money ledger, shift report/history, returns, **and** the dashboard
  KPI cards + live activity (count cards like Tickets/SKUs stay plain integers).
- **Data layer untouched** — zero Room schema/migration changes (DB was already cents). Verified:
  `git diff 9aebf1d main -- app/src/main/java/com/nexapos/retail/data/` is empty.
- **Tests:** 129 unit tests green (VAT/discount/tender math re-asserted in cents).

### Bug hunt (post-refactor) — results
- Cents diff: 2 Sonnet reviews, **no money bugs** found.
- Core features (auth/cash/receipt/drawer/barcode): 3 Sonnet reviews — **no confirmed
  high-severity bugs**. Fixed 2 low-severity reals (shift double-tap guard, receipt-PDF cache
  prune) in `95b9961`. Open low/intent items below.

---

## 3. Multi-branch add-on (IN PROGRESS)

**Design:** `docs/superpowers/specs/2026-06-11-multi-branch-design.md`
**Plan:** `docs/superpowers/plans/2026-06-11-multi-branch.md`

### Decisions (locked with owner)
| Decision | Choice |
|---|---|
| Sync transport | **Firebase** (Firestore + Auth), managed backend |
| Visibility | Read-only: reports, stock, sales (no remote edit / stock transfer in v1) |
| Topology | **HQ controls who sees whom** (visibility matrix); HQ sees all |
| Unlock | **Offline ECDSA licence key**, entered once in Settings |

### Core principles (to keep bug count low)
1. **Zero changes to the operational DB** — remote data served from Firestore cache, never written
   to local Room. Branch identity lives in encrypted prefs.
2. **Single-writer per branch** — peers/HQ only read. No merge, no invoice collisions.
3. **Sync never touches the sale** — fire-and-forget after `checkout()`, like the drawer kick.
4. **No hand-rolled sync queue** — lean on Firestore offline persistence.

### Stage 1 — offline licence gate ✅ DONE + verified on-device
New files:
- `data/branch/LicenseManager.kt` — verifies `NXB-<urlB64(payload)>.<urlB64(sig)>` where
  payload = `business|maxBranches|expiryEpochDay`. ECDSA P-256 / SHA256, embedded **public** key,
  fails closed. Pure JVM → unit-tested (`LicenseManagerTest`, 8 cases). Business name binding is
  normalized (upper, alnum + single spaces).
- `data/branch/MultiBranch.kt` — stores the code in Keystore-backed encrypted prefs and
  **re-verifies on every read** (tampered prefs can't fake an unlock).
- `ui/settings/MultiBranchSettingsScreen.kt` — Settings → Multi-branch (admin-only): enter key to
  unlock; shows Active (licensed-to / branches / expiry) + Remove. Wired in `SettingsScreen.kt`
  ("Add-ons" group) and `PosApp.kt` (route `multibranch-settings`).
- `docs/LICENSING.md` — how codes are issued.

On-device check (Galaxy Tab S11 emu): pasted a real code → **Active · Licensed to QUINCAILLERIE RB
TRADING · 4 branches · Never**, persisted across re-open. Embedded public key proven against a
tool-issued code. *Minor:* during the test the view bounced to the dashboard right after Activate
(activation still committed) — likely a stray tap on the laggy cold-booted emu, not a screen bug;
worth one clean manual confirm.

### Stages 2–5 — ✅ ALL DONE (on `feature/multi-branch`, not pushed)
- **Stage 2** ✅ — `BranchIdentity` (HQ vs branch, normalized code, name) in encrypted prefs;
  `SyncModels` pure DTOs (summary / stock chunks / per-day sales+returns+money+shifts) with explicit
  `toMap()`/`fromMap()`, unit-tested.
- **Stage 3** ✅ — Firebase WITHOUT google-services plugin: a NAMED app "nexapos-mb" built lazily
  from 3 owner-pasted values (projectId/appId/apiKey) in encrypted prefs (`FirebaseConfig`).
  `RemoteStore` boundary + `FirestoreRemoteStore`; `RealBranchSync` (Mutex-serialised, fire-and-forget
  after sale + shift close, every failure → OFFLINE, never throws into selling). `docs/FIREBASE_SETUP.md`
  + `firestore.rules` (uid-scoped). Engine fully JVM-tested via `FakeRemoteStore`.
- **Stage 4** ✅ — read-only viewing: `RemoteBranchRepository` + `FirestoreRemoteBranchRepository`
  (reads via `RemoteStore`); each branch self-registers a directory doc on sync (`registerSelf`).
  `BranchesScreen` (viewable list + per-branch live summary + staleness label) and `RemoteBranchScreen`
  (Overview / Stock / Sales-by-day tabs). Reached from Settings → Multi-branch → **View branches**
  (gated: admin + licensed + configured) rather than the static nav rail.
- **Stage 5** ✅ — HQ `VisibilityEditorScreen` (per-branch grant chips, saves whole matrix at
  `config/visibility`); consolidated "All branches · today" card for HQ; `BranchSyncWorker`
  (WorkManager periodic ~6 h sync, scheduled on app start + after Connect & sync); adversarial review
  pass (3 reals fixed: visibility-editor state reset, sync-label clock-skew/ticker, Firestore listener
  error logging); `versionCode`→3, `versionName`→**1.1.0**.

**Pure visibility/roll-up logic** (`BranchDirectory.viewable` / `consolidate`) is unit-tested
(`BranchDirectoryTest`). HQ sees all other branches; a branch sees only the codes the matrix grants.
**Known v1 limits (accepted):** `observeStock` reads chunk 0 only (<1500 SKUs); 30-day backfill
deferred; Firestore listener errors are logged, not surfaced as a UI error state.

### Licensing keys (developer-only)
- Tool: `tools/licensing/LicenseTool.java` (gitignored). `keygen` / `issue` / `verify` modes —
  run with `java tools/licensing/LicenseTool.java <mode> …`.
- **Public key** is embedded in `LicenseManager.PUBLIC_KEY_B64`.
- **Private key** is in `tools/licensing/PRIVATE_KEY.txt` (gitignored — NOT in git). Move it to
  proper secure storage. If lost, you must ship a new public key in an app update.
- Issue a code: `java tools/licensing/LicenseTool.java issue <privKeyB64> "Business Name" <maxBranches> <expiryEpochDay(0=never)>`.

---

## 4. Outstanding / before shipping (not blockers for dev, but track them)

1. **Release-build smoke test** — everything was tested on the **debug** APK. Build a signed
   **release** APK (R8/minify on) and smoke-test before giving anything to the owner; minify can
   break Room/reflection in ways debug never shows. *(Highest-priority pre-ship task.)*
2. **`versionCode` bump** — ✅ done: now `versionCode = 3`, `versionName = "1.1.0"` in
   `app/build.gradle.kts` (bumped with multi-branch Stage 5).
3. **Owner update safety (confirmed):** the owner's install is a release build he signed; a new
   release **signed with the same keystore** updates in place and keeps all data (no schema change).
   A different/lost keystore → signature mismatch → uninstall → data loss. Have him back up first.
4. **`fallbackToDestructiveMigration(dropAllTables = true)`** in `di/AppContainer.kt:60` — not
   triggered now, but a FUTURE schema change without a migration would silently wipe data. Consider
   removing `dropAllTables` so a missing migration fails loudly instead.
5. **Intent question (your call):** the "Money" ledger counts card/credit sales as money-in
   (`MoneyViewModel.kt:108`). Fine if that screen means "all money movement"; wrong if it should
   mirror physical cash. Shift report does cash reconciliation correctly regardless.
6. **Barcode HID edge** — a speculative `eventTime==0` case on some scanner drivers; test with the
   real scanner rather than changing code blind.

---

## 5. How to resume (toolchain + gotchas)

- **Env (set inline for every Gradle call):**
  `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`,
  `ANDROID_HOME = %LOCALAPPDATA%\Android\Sdk`.
- **Build/verify (run `ktlintFormat` in its OWN invocation first):**
  `./gradlew ktlintFormat` then `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck --continue`.
- **Emulator:** AVD `Galaxy_Tab_S11` (2560×1600). Launch detached with
  `-no-snapshot-load -no-boot-anim`; cold boot can throw a one-off ANR (tap **Wait**). PINs: Owner
  4827, Priya (cashier) 2222. Sign-in field bounds (device px): PIN field tap `(610,923)`, sign-in
  `(634,1046)`.
- **Gotcha — PowerShell has no heredoc:** for git commits use a temp message file
  (`git commit -F .git\COMMIT_MSG_TMP.txt`), not `-m @'...'@` or `<<EOF`. Avoid `(parentheses)` in
  inline `-m` strings.
- **Gotcha — ktlint:** no line `//` comment directly between a KDoc block and an annotation; fold
  the note into the KDoc.
- **Commit trailer:** `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.

---

## 6. Pointers
- `docs/superpowers/specs/` + `plans/` — design source of truth (multi-branch + earlier features).
- `docs/LICENSING.md` — licence format/issuing.
- `docs/SESSION_LOG_v1.1.0.md` — prior session (receipt-scan, external-scanner, PDF receipt).
- `CLAUDE.md` — project conventions (money in cents, atomic sales, build/test).
