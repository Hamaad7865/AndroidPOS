# NexaPOS Retail — Session Log (v1.0.0 client-release prep)

**When:** June 2026
**Outcome:** First **signed release** APK built, audited, verified on an 8.7″ tablet emulator, and handed off for the client to test. Final fixes opened as **PR #1**.

> This is a chronological work log for the session. It records what changed, why,
> where the artifacts are, and what's still pending — so context survives a compaction.

---

## 0. TL;DR / current state

- App = **NexaPOS Retail**, native Kotlin + Jetpack Compose Android POS, fully offline + SQLCipher-encrypted. Single Gradle module (`:app`), `applicationId com.nexapos.retail`, minSdk 26 / target 35.
- **Version is now 1.0.0** (`versionCode 2`). DB schema is **v6**.
- **Signed release APK ready to ship:** `C:\Users\sheik\Downloads\nexapos-retail-v1.0.0.apk` (15.9 MB).
- Client install note: `C:\Users\sheik\Downloads\nexapos-client-instructions.txt`.
- Sample import file: `C:\Users\sheik\Downloads\nexapos-products-sample.csv`.
- **PR:** https://github.com/Hamaad7865/AndroidPOS/pull/1 (branch `release/v1.0.0`, commit `0ef118c`, into `main`).
- Test device emulator: **`Galaxy_Tab_A11`** AVD (8.7″, 1340×800 @ 179 dpi).

### ⚠️ Must-not-forget
- **Back up the signing keystore + password** (see §6). Losing it = can never update the app on the client's device.
- Before shipping an update **over real client data**, switch the DB from `fallbackToDestructiveMigration` to a **real Room migration** (schema is already exported under `app/schemas/`).

---

## 1. Parties: Supplier detail "New Sale" → "New Purchase Order"

**Problem:** In Parties → Suppliers, the right-hand detail panel showed a **"New sale"** button for suppliers, which makes no sense (you buy *from* a supplier, not sell *to* them).

**Fix:** Made the action button context-aware in `PartiesScreen.kt`:
- **Customer** → 🛒 "New sale" → POS.
- **Supplier** → 🚚 "New purchase" → New Purchase Order (uses `PosIcons.truck`, the app-wide Purchase icon).
- Empty-state hint reworded from "…start a sale" to "…recent activity".

**Files:** `ui/parties/PartiesScreen.kt`, `ui/PosApp.kt` (added `onNewPurchase`).

---

## 2. Test images in emulator + Supplier autofill on "New purchase"

**(a) Random product images for assigning to products**
- Generated 10 labeled JPGs (Coca-Cola, Pepsi, Bread, Fresh-Milk, Rice-5kg, Cooking-Oil, Soap, Shampoo, Biscuits, Water-1.5L) via PowerShell `System.Drawing`.
- Pushed to `/sdcard/Pictures/nexapos_imgs/` and indexed into **MediaStore** (so the image picker shows them).
- These live on the **emulator only**.

**(b) Supplier autofill when starting a PO from Parties**
- Parties → Supplier → "New purchase" now passes the supplier id via route `add-purchase?supplierId=X`.
- `AddPurchaseScreen` gained `initialSupplierId` and a **race-safe prefill** effect (waits for the supplier list to load, fills name/contact/locality once, guarded so a later refresh can't clobber edits).

**Files:** `ui/purchase/PurchaseForms.kt` (`AddPurchaseScreen` prefill), `ui/parties/PartiesScreen.kt`, `ui/PosApp.kt` (`add-purchase?supplierId={supplierId}` with `NavType.LongType`, default −1).

---

## 3. Money module rebuild (it was "dead")

**Problem:** The Money landing tab ("Cash & Bank") and the **Ledger** tab were static placeholders — they never showed real data; lots of dead scaffolding.

**Fix (rewrote `MoneyScreen.kt` + `MoneyViewModel.kt`):**
- **Money hub** → live dashboard: *This month* Income / Expenses / **Net**, an **Accounts** rollup (grouped by the account label on manual entries), and **Recent activity**.
- **Ledger** → real combined cash journal (sales + manual income/expense) with a **running balance**, a **Today** filter, and **Excel/PDF export**.
- **Income tab** now includes real **sales takings** (so the month total matches the list).
- Removed dead code (`Account`, `AccountCard`, `RecentRow`, `moneyAccounts`, unused `recent` feed).
- Reused the existing `ExportButtons` (Excel/PDF) from `ui/reports/ReportExport.kt`.

**Verified live:** dashboard math (income = June sales 460, expense 200, net 260) and ledger running balance (690 → 1,150 → 1,380 → 1,610 → 1,410) were correct on screen.

**Files:** `ui/money/MoneyScreen.kt`, `ui/money/MoneyViewModel.kt`, `ui/PosApp.kt` (pass `vm` to `MoneyHubScreen` + `LedgerScreen`).

---

## 4. CSV product import

**Goal:** Client can upload a CSV to import all products.

**Built:**
- **Import** button (new `PosIcons.upload` icon) on the Products page → help dialog → system file picker → parse → result summary dialog.
- **Pure parser** in `ui/products/ProductDocs.kt`: `parseProductsCsv` + `parseCsvTable` (RFC-4180 quoting, escaped `""`, UTF-8 BOM, CRLF/LF). Required columns **Name + Price**; SKU/Barcode/Category/Cost/Stock optional; tolerant headers (`Price (Rs)`, `Qty`, etc.).
- **`CatalogViewModel.importProductsFromCsv`**: matches existing products by **barcode → SKU → name** (updates them), inserts the rest, **auto-creates categories**, skips invalid rows with per-line notes.
- Round-trips the existing **Export** format.

**Verified live:** imported a 5-row file → "Added 4 · Skipped 1", categories auto-created, quoted comma-in-name parsed, price-less row skipped.

**Files:** `ui/components/PosIcons.kt` (upload icon), `ui/products/ProductDocs.kt`, `ui/products/CatalogViewModel.kt`, `ui/products/ProductsScreen.kt`. Sample: `Downloads/nexapos-products-sample.csv`.

---

## 5. Navigation: Cancel/Back → parent module (not Home) + POS category scroll

**Problem:** Cancel/Back on some sub-screens jumped to **Home** instead of the parent module. Cause: the shared `go` nav helper always did `popUpTo("home")`, collapsing the back stack for screens reached through it (Money add-income/expense, Sales list from POS).

**Fix (`ui/PosApp.kt`):** `go` now only resets to Home for **top-level module routes** (`MODULE_ROUTES` = home, pos, products, parties, purchase, money, income, expense, ledger, reports, settings). Any other route (add/edit/detail) is pushed on top of the current module, so `popBackStack()` returns to it.

**POS category chips:** already had `.fillMaxWidth().horizontalScroll(...)` — verified it scrolls once there are many categories (imported extras → 14 categories overflowed and scrolled).

**Files:** `ui/PosApp.kt` (`MODULE_ROUTES` + branched `go`).

---

## 6. Emulator / device profile work

Walked through several display targets before landing on the client's actual device:

| Attempt | Spec | Result |
|---|---|---|
| Galaxy Tab S11 (first guess) | 2560×1600 @ 280 dpi | "too small" (1462 dp) |
| same, 320 dpi | 2560×1600 @ 320 | comfortable (1280 dp) but reports 9.4″ |
| "make it 11 inch" | 2560×1600 @ 274 dpi | true 11.0″ |
| **"it's an A11"** (final) | **1340×800 @ 179 dpi** | **true 8.7″** ✅ |

**Final AVD: `Galaxy_Tab_A11`** — Samsung Galaxy Tab A9/A11 (8.7″, 800×1340 portrait, ~179 ppi).
- Created from the `pixel_tablet` base, then config edited: `hw.lcd.width=1340`, `hw.lcd.height=800`, `hw.lcd.density=179`, `hw.initialOrientation=landscape`, `hw.keyboard=yes`, `hw.ramSize=4096`, `showDeviceFrame=no`.
- Renamed `Galaxy_Tab_S11` → `Galaxy_Tab_A11` (folder + `.ini` path).
- **Key math:** an 11-inch 16:10 screen is *always* ~1462–1495 dp wide; the app's comfortable layout (1280 dp) maps to ~9.4″. 8.7″ @ 179 dpi gives ~1198 dp — close to the design target, looks great.

**Launch it:** `D:\Android\Sdk\emulator\emulator.exe -avd Galaxy_Tab_A11` (or Android Studio Device Manager).
SDK lives on **D:** (`D:\Android\Sdk`), AVDs at `D:\Android\.android\avd`, JDK 17 at `D:\Android\jdk17`. adb is `D:\Android\Sdk\platform-tools\adb.exe`.

---

## 7. Full button / QA audit (before client handoff)

Ran **5 parallel auditor agents** over every screen (~150+ interactive elements). Findings:

- ✅ **1 dead button:** Settings → "Help" (no handler).
- ⚠️ **1 data-loss bug:** New PO "Expected delivery" + "Internal notes" accepted text but were dropped on save.
- ℹ️ **1 placeholder:** "Purchase Return" report tile → "coming soon" card.
- Everything else (all nav, dialogs, exports, POS/checkout/receipt, etc.) correctly wired.

### Fixes (all verified live on the A11)
1. **Settings "Help"** → opens a **Help & support** dialog (`SettingsScreen.kt`).
2. **PO notes + expected delivery** → persisted and shown on the PO detail.
   - Added `expectedDelivery` + `notes` to the `Purchase` entity → **DB schema v6** (`PosDatabase.kt` version bump; `app/schemas/.../6.json` exported).
   - Threaded through `PurchasesViewModel.recordPurchase` and the `AddPurchaseScreen.confirm()` call; displayed in `DetailHeaderCard`.
   - Verified: PO-1043 detail showed *"Fri 06 Jun"* + *"Call before delivery"*.
3. **Removed** the "Purchase Return" report tile (`ReportsScreen.kt` `SECTIONS`).

---

## 8. Signed release build + the SQLCipher crash

**Advice given:** do **not** ship the debug APK. The debug build is debug-key-signed; if the client installs it and later gets the real release build (different key), Android blocks the update (signature mismatch) → uninstall = data loss. Ship a **signed release** from the start; keep that key forever.

**Steps done:**
1. **Generated the keystore** (no keystore existed; `build.gradle.kts` already had a release signing config reading `keystore.properties`).
2. **Bumped version** to 1.0.0 / `versionCode 2`; aligned the in-app "v" labels.
3. **Built `assembleRelease`** (R8 minify + shrink + sign). Shrank 34.5 MB (debug) → **15.9 MB**.
4. 🛑 **Release crashed on launch** (SIGABRT) — `NoSuchFieldError: no "J" field "mNativeHandle" in net.sqlcipher.database.SQLiteDatabase`. R8 obfuscated SQLCipher; `libsqlcipher.so`'s `JNI_OnLoad` resolves Java fields **by name** → not found → abort on first DB open. **Debug builds masked this.**
5. **Fix:** added to `app/proguard-rules.pro`:
   ```
   -keep class net.sqlcipher.** { *; }
   -keep interface net.sqlcipher.** { *; }
   -dontwarn net.sqlcipher.**
   ```
6. **Rebuilt + re-verified:** signature is ours; on a fresh install the release completes the setup wizard, sets a PIN, opens the encrypted Room DB, and reaches Home — **no crash**.

> Lesson: **always smoke-test the release (minified) build on a device** — debug builds hide R8/ProGuard issues, especially with native/JNI libs (SQLCipher).

---

## 9. Release artifacts & the signing key

**APK to upload:** `C:\Users\sheik\Downloads\nexapos-retail-v1.0.0.apk` (signed release; the old debug `nexapos-retail.apk` was deleted to avoid uploading the wrong one).

**Signing keystore (BACK THIS UP — cloud + offline):**
- File: `D:\Personal\Android App\POS\nexapos-release.jks`
- Store/key password: `NexaPOS-RB-2026`
- Alias: `nexapos`
- Cert SHA-256: `52:E1:B7:D0:B6:B0:CF:54:…`
- `keystore.properties` (gitignored) points the build at it.

**Client install steps** (in `nexapos-client-instructions.txt`): tap link → download → "Allow this source" → Play Protect "More details → Install anyway" → first run sets shop details + PIN. Works on Android 8+. Hosting tip: avoid sending `.apk` over WhatsApp (it blocks them); use a direct download link.

---

## 10. App modules (functional)

8 nav-rail modules + onboarding:
1. **Home** (dashboard KPIs + live activity)
2. **POS / Sale** (cart, hold/resume, checkout: cash/card/mobile/credit, receipt: print/SMS/WhatsApp/PDF, sales list, returns)
3. **Products** (catalog, add/edit full fields, CSV import/export, print EAN-13 labels)
4. **Parties** (customers & suppliers, statements, new sale / new purchase)
5. **Purchase** (PO list, new PO with supplier autofill + notes/delivery, PO detail + status)
6. **Money** (Cash & Bank, Income, Expenses, Ledger — all live; export)
7. **Reports** (15 reports across Transactions / Financial / Money / Per-product, each with period filters + Excel/PDF)
8. **Settings** (profile, theme, encrypted backup/restore, PIN, printing, Help)
- **Onboarding:** Splash → Login (PIN) → first-run setup wizard.

---

## 11. PR / git

- Branch `release/v1.0.0` (off `main`), commit `0ef118c`, **PR #1**: https://github.com/Hamaad7865/AndroidPOS/pull/1
- **9 files** in the PR (the §7–8 finalize work + `schemas/6.json`):
  `app/build.gradle.kts`, `app/proguard-rules.pro`, `data/PosDatabase.kt`, `data/entity/Purchase.kt`,
  `ui/purchase/PurchaseForms.kt`, `ui/purchase/PurchasesViewModel.kt`, `ui/reports/ReportsScreen.kt`,
  `ui/settings/SettingsScreen.kt`, `app/schemas/com.nexapos.retail.data.PosDatabase/6.json`.
- Earlier session work (§1–6: Parties, Money, import, nav) was already committed on `main` before this batch.
- **Deliberately excluded:** `keystore.properties` / `*.jks` (gitignored), and `.github/` (left untracked — the GitHub token lacks `workflow` scope, so including the CI workflow would block the push).

---

## 12. Open offers / follow-ups (not yet done)

- Load **sample products** into a build so the client demo isn't blank.
- Wire the Dashboard **"Print labels"** quick-tile to print directly (currently opens Products, where the real button is).
- **Real Room migration** (replace `fallbackToDestructiveMigration`) before any update over live client data.
- Add the **CI workflow** (`.github/`) via the GitHub web UI or a token with `workflow` scope.
- Optional: one-page **feature sheet** (PDF/Word) for the client.

---

## 13. Key technical learnings (reusable)

- **R8 + SQLCipher:** keep `net.sqlcipher.**` verbatim or the native JNI aborts on first DB open. Always smoke-test the minified release.
- **Compose modifier order:** width caps use `widthIn(max=X).fillMaxWidth()` (not the reverse). A whole field is tappable via `clickable(...) { focus.requestFocus() }` + `BasicTextField(Modifier.fillMaxWidth().focusRequester(focus))`.
- **Tablet sizing:** dp width ≈ physical_inches × 160 regardless of resolution; an 11″ 16:10 panel is ~1462–1495 dp. Pick density to hit the app's design dp.
- **Signature lock-in:** the first signing key is permanent for that app id; debug vs release keys can't update each other.
- **Bash on Windows/MSYS:** prefix adb shell paths with `MSYS_NO_PATHCONV=1`; use `git -C <path>` instead of `cd`.
