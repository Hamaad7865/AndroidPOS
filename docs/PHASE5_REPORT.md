# NexaPOS Retail — Phase 5 Report (Offline Backend)

Phase 5 turned the design-verified prototype into a real, offline, encrypted POS.
**Zero recurring fees, no cloud account, no Play Store required.** All five planned
items are implemented and the project builds clean (`assembleDebug` + unit tests +
ktlint + detekt all green).

---

## What was delivered

### 1. Real persistence (Room/SQLite) — replaced all in-memory data
- **Selling flow is fully real**: the POS reads live products; completing a sale
  **persists it atomically, decrements stock, and assigns sequential invoice
  numbers** (`S-00001`, `S-00002`, …). Verified math unchanged (e.g. 690 + 15% VAT = **794**).
- **Products**: live list (real SKU count, stock value); **Add Product → Publish**
  inserts a real product (name, SKU, category, price, opening stock, photo).
- **Parties**: live customers/suppliers list + **Add customer/supplier** dialog.
- **Purchases**: live list; **New Purchase → Confirm** records a purchase and
  **raises stock** for matching products.
- **Money**: live Income & Expense lists + **Add income / Record expense** save real
  cash-book entries; monthly totals are computed from real sales + entries.
- **Dashboard**: KPI numbers are live (Today's sales, Stock value, Items in stock,
  Categories, Low-stock count, Suppliers).
- Reconciled the earlier duplication: retired the unused generic `SaleViewModel` /
  `CheckoutScreen`; the design-verified POS/Checkout/Receipt screens are now the
  single, Room-backed selling path.
- Demo data is seeded on first run so every screen looks alive immediately.

### 2. Encryption at rest (SQLCipher)
- The entire database is encrypted with **SQLCipher (AES-256)**.
- The key is a random passphrase generated once and stored in an **Android
  Keystore-backed EncryptedSharedPreferences** — it never leaves the device.
- The passphrase is a *portable* passphrase (SQLCipher writes its salt into the
  file header), so an encrypted backup can be restored onto another device by
  entering the **recovery key** (Settings → Data & security → Show recovery key).

### 3. Local staff PIN login with roles (no cloud auth)
- Login requires a **staff PIN**, verified locally. The PIN alone identifies the
  staff member (no name picker), so PINs are unique across active staff.
- PINs are stored only as **PBKDF2(HMAC-SHA256, 120k iterations) + per-staff
  random salt** hashes in the encrypted DB (`staff` table, schema v12) — never in
  clear text. Failed attempts feed a shared brute-force lockout.
- **Two roles**: **ADMIN** sees everything; **CASHIER** runs the till but never
  sees cost, margin or profit — the profit reports (Bill-wise Profit, Profit &
  Loss, Product Purchase History), the product COST column, the cost/margin
  fields on Add/Edit Product, and the cost column in the CSV export are all
  hidden. The rules live in `domain/StaffPolicy.kt` (pure JVM, unit-tested).
- **Settings → Staff & roles** (admin-only): add cashiers/admins, rename, change
  role, reset a PIN, deactivate (deactivated staff keep their sales history but
  can't sign in). The repository enforces PIN uniqueness and refuses to demote
  or deactivate the **last active admin**.
- **No default PIN**: the first-run wizard creates the **Owner (admin)** from the
  PIN you choose. A pre-roles install keeps signing in with its legacy shop PIN,
  which is promoted to the Owner admin record on first login.

### 4. Backup & restore (free, offline)
- **Backup** writes an encrypted copy of the database to a folder **you choose**
  via the Android file picker — a **USB/SD card or a Google-Drive-synced folder**.
  No Google Cloud project, no API keys, no subscription.
- **Restore** picks a backup file, (optionally) takes the recovery key for
  cross-device restores, then swaps it in and restarts.
- **Automatic daily backup** runs via WorkManager once a destination folder is set.
- Android's own cloud auto-backup is disabled (`allowBackup=false`) so the encrypted
  DB is only ever backed up the way you control.

### 5. Product images as files (never DB blobs)
- Picked images are **downscaled to ≤1024px JPEG and stored as files** under app
  storage — never as database blobs. This keeps the DB small and the routine DB
  backup tiny. Images are intentionally **excluded from the DB backup** (they live
  outside the database).

---

## How to test it (quick tour)
1. **Login**: enter your **staff PIN** → Sign in (the first-run wizard sets the
   Owner/admin PIN).
2. **Sell**: POS → tap products → **Charge** → keypad → **Complete sale** → receipt.
   Re-open POS: the sold item's stock is lower. Dashboard "Today's sales" went up.
3. **Add a product** (Products → Add product): tap the photo box to pick an image,
   fill name/SKU/price/stock → **Publish**. It appears in the list and in the POS.
4. **Record an expense** (Money → Expenses → Record expense) → it appears in the list.
5. **Add a customer** (Parties → Add party).
6. **Record a purchase** (Purchases → New purchase → Confirm) → stock goes up.
7. **Encryption**: data survives a full app close/reopen; the DB file on disk is not
   readable as plain SQLite.
8. **Roles** (Settings → Staff & roles, admin only): add a cashier, sign out and
   sign in with the cashier's PIN — profit reports and cost prices disappear.
9. **Backup** (Settings → Data & security): Choose folder → Backup now. Then try
   Restore.

---

## How to deploy (sideload — no Play Store)

### Quick install (debug build)
```
gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or copy `app-debug.apk` to the tablet and tap it (enable "Install unknown apps").

### Production install (signed release — recommended for shop tablets)
1. **Create a signing key once** (keep the .jks safe — you need it for every update):
   ```
   keytool -genkeypair -v -keystore nexapos-release.jks -keyalg RSA -keysize 2048 ^
     -validity 10000 -alias nexapos
   ```
2. **Create `keystore.properties`** in the project root (already gitignored):
   ```
   storeFile=C:/path/to/nexapos-release.jks
   storePassword=********
   keyAlias=nexapos
   keyPassword=********
   ```
3. **Build the signed, minified APK**:
   ```
   gradlew assembleRelease
   ```
   → `app/build/outputs/apk/release/app-release.apk`
4. **Install** on each tablet: `adb install -r app-release.apk`, or copy the file and
   tap to install.
5. **Updates**: bump `versionCode`/`versionName` in `app/build.gradle.kts`, rebuild,
   `adb install -r` (this keeps existing data).

### First-run notes for a shop
- First launch seeds demo data so screens aren't empty — you can delete it later via
  Settings → Danger zone (wiring of the delete button is a follow-up; for now a clean
  start = reinstall).
- The first-run wizard sets the **Owner (admin) PIN** — there is no default PIN.
  Give each till worker their own **cashier PIN** (Settings → Staff & roles).
- Set a **backup folder** (a USB stick, SD card, or a folder the Google Drive app
  syncs) and write down the **recovery key**.

---

## Known limitations / sensible follow-ups
- **Forms**: the highest-value forms (Add Product, Add Income/Expense, Add Party,
  New Purchase) are fully wired. Some secondary fields on those forms (brand, rack,
  batch, credit terms, etc.) are display-only — they aren't in the data model yet.
- **Analytics still illustrative**: the Dashboard 14-day chart, "Top movers", "Live
  activity", the Reports analytics, and the Money "Accounts"/"Ledger running balance"
  still show sample figures (the headline KPI numbers are real). Wiring these to real
  aggregates is a follow-up.
- **Product photos** show on the Add-Product screen; showing them as thumbnails in the
  product list / POS tiles needs an image cache for smooth scrolling (follow-up).
- **Schema migrations**: the schema is now at **version 12** with additive Room
  migrations (latest: v11→v12 adds the `staff` table), so updates install over
  existing data without loss.
- **Multi-counter LAN sync** is deliberately deferred (post-Phase-5).
- **Test the restore flow** end-to-end on your device before relying on it.

## Build environment
`JAVA_HOME=D:\Android\jdk17`, `ANDROID_HOME=D:\Android\Sdk`,
`GRADLE_USER_HOME=D:\Android\.gradle`. Build loop:
`gradlew ktlintFormat -q` then `gradlew assembleDebug testDebugUnitTest ktlintCheck detekt --continue`.
