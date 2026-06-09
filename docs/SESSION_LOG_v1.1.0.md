# NexaPOS — Session Log (post‑v1.0.0 features)

**Date:** 2026‑06‑08 · Continues from `docs/SESSION_LOG_v1.0.0.md`.
**Repo:** `D:\Personal\Android App\POS` · remote `Hamaad7865/AndroidPOS` · default branch `main`.
**App:** native Kotlin / Jetpack Compose POS, fully offline, SQLCipher‑encrypted. `applicationId com.nexapos.retail`, minSdk 26 / target 35.

---

## 0. TL;DR — current state

This session (after v1.0.0) delivered, in order:

1. **Git cleanup** — deleted the merged `release/v1.0.0` branch (local + remote), fast‑forwarded local `main`.
2. **Fix: uniform POS product‑card heights** (no more staircase).
3. **Fix (CRITICAL): CSV product import was 100% broken** — a BOM check that chopped the first char off every file. Was shipping broken in v1.0.0.
4. **Feature: Receipt scanner** — photograph a supplier receipt → on‑device ML Kit OCR → editable review → register a Purchase (supplier + items + stock + catalog sync). Includes **row‑grouping** and a **column‑aware invoice parser** for formal multi‑column VAT invoices.
5. **Feature: Styled PDF receipt share** — the POS Sale receipt shares as a styled PDF slip (with BRN + VAT), not plain text.
6. **Feature: External (HID) barcode scanner** — **IN PROGRESS, brainstorming. Design presented, AWAITING USER APPROVAL.** Nothing built yet.

### GitHub PRs
| PR | Title | State | Branch | Head |
|----|-------|-------|--------|------|
| **#2** | Receipt scan + card‑height & CSV‑import fixes | **MERGED → `main`** (`e14011d`) | `feature/receipt-scan` | `8ea8b05` |
| **#3** | Column‑aware parsing for formal multi‑column invoices | **OPEN** | `feature/receipt-scan` | `b1da2c4` |
| **#4** | Share the POS sale receipt as a styled PDF (BRN + VAT) | **OPEN** | `feature/pdf-receipt` | `80e68bc` |

- `https://github.com/Hamaad7865/AndroidPOS/pull/2` (merged)
- `https://github.com/Hamaad7865/AndroidPOS/pull/3` (open — column‑aware parser)
- `https://github.com/Hamaad7865/AndroidPOS/pull/4` (open — styled PDF receipt)

**Currently checked‑out branch: `feature/pdf-receipt`.** The external‑scanner work has no branch/commits yet (mid‑brainstorm).

---

## 1. Git / branch reference

- `release/v1.0.0` was merged via PR #1 (prior session) then **deleted** here (local + remote). Local `main` fast‑forwarded to `c0e4ffe`, later `origin/main` advanced to `e14011d` (PR #2 merge).
- `feature/receipt-scan` (off the old main) holds the whole receipt feature + the 2 fixes. PR #2 merged it at `8ea8b05`; the later `b1da2c4` (column‑aware) sits on the same branch as PR #3. GitHub auto‑deleted the remote branch on PR #2 merge, then a re‑push recreated it for PR #3.
- `feature/pdf-receipt` branched off `origin/main` (post‑merge) so it does **not** tangle with PR #3.

### Commit list on `feature/receipt-scan` (PR #2 then #3)
```
b1da2c4 feat: column-aware parsing for formal multi-column invoices   (PR #3)
8ea8b05 feat: group same-row OCR fragments so printed receipts auto-extract items
e3a4956 style: ktlintFormat + detekt fixups for receipt scan
410705b feat: receipt scan screen + Purchase-screen entry + route
da714d1 feat: add ReceiptScanViewModel (OCR + editable draft + register)
c0b0a3c build: add FileProvider for in-app receipt camera capture
fbeaf87 feat: add ML Kit receipt OCR wrapper
adc519d refactor: extract shared recordPurchaseFromDraft, reuse in PurchasesViewModel
503fe51 feat: add unit-tested receipt OCR-line parser
17532e7 feat: add pure receipt draft data types
978dae4 build: add ML Kit bundled text-recognition for receipt OCR
43c723e docs: receipt-scan design spec + implementation plan
ab22b48 fix: CSV import rejected every file (empty-string BOM check)
1ba6228 fix: uniform POS product card heights
```
### `feature/pdf-receipt` (PR #4)
```
80e68bc feat: share the POS receipt as a styled PDF slip (BRN + VAT)
```

---

## 2. Fix — POS product card heights

`app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt`, `ProductCard`'s name `Text`: changed `Modifier.heightIn(min = 34.dp)` → `minLines = 2, maxLines = 2` so 1‑line and 2‑line names render the same height at any font scale (image 96dp + price row were already fixed). Removed the now‑unused `heightIn` import. **Verified on emulator.**

## 3. Fix — CSV import BOM bug (CRITICAL)

`app/src/main/java/com/nexapos/retail/ui/products/ProductDocs.kt`, `parseCsvTable`. The BOM‑strip read `text.startsWith("")` — the U+FEFF char had been lost from source, leaving an **empty string literal, which `startsWith` always matches** → `text.substring(1)` chopped the first character off **every** CSV → `"Name"` header became `"ame"` → every import failed with "needs Name and Price". Fixed:
```kotlin
val s = if (text.firstOrNull()?.code == 0xFEFF) text.substring(1) else text
```
(Compares the code point with a plain hex literal — no invisible char; the JSON/edit pipeline kept turning `﻿` back into a literal BOM, hence the code‑point approach.) **This was shipping broken in v1.0.0.** Verified: import works on device.

---

## 4. Feature — Receipt scanner (PR #2 merged; PR #3 open)

**Spec:** `docs/superpowers/specs/2026-06-08-receipt-scan-design.md` · **Plan:** `docs/superpowers/plans/2026-06-08-receipt-scan.md`.

**Flow:** Purchase screen → **Scan receipt** → take photo / pick image → **on‑device ML Kit OCR** (bundled, offline) → `ReceiptParser` → **editable review** (each line tagged *In catalog* / *New*) → **Register** → `recordPurchaseFromDraft` creates the supplier, creates/links products, records the Purchase, raises stock. **No DB schema change** (receipt photo via existing `ImageStore`, keyed by code) so updates don't wipe data.

### Files
**Created (`app/src/main/java/com/nexapos/retail/ui/purchase/receipt/`):**
- `ReceiptModels.kt` — `OcrLine(text, top, bottom, left, right)`, `ReceiptDraftLine(name, quantity, unitCostRupees)`, `ParsedReceipt(supplierGuess, lines, warnings)`.
- `ReceiptParser.kt` — **pure, unit‑tested.** `parse()` tries `parseTable()` (column‑aware, see §4.2) then falls back to `parseSimple()` (row‑merge + trailing‑price for till receipts). Helpers: `mergeRows`, `columnKind` (data‑driven `COLUMN_LABELS` regex table), `detectSupplier`, `parseMoney`, `parseQty`, `fit`.
- `ReceiptOcr.kt` — ML Kit `TextRecognition` wrapper: `recognise(context, uri): List<OcrLine>` (suspend, `InputImage.fromFilePath`).
- `ReceiptScanViewModel.kt` — `ScanPhase` (IDLE/PROCESSING/REVIEW/DONE), `onImageCaptured` (saves via `ImageStore`, runs OCR+parse), editable `lines`, `isKnown`, `total`, `register` → `recordPurchaseFromDraft`.
- `ReceiptScanScreen.kt` — one‑screen flow: camera (`TakePicture` + FileProvider) / gallery (`GetContent`), spinner, editable review rows, Register.

**Created (`ui/purchase/`):** `PurchaseRecorder.kt` — `suspend fun recordPurchaseFromDraft(purchasesRepo, catalogRepo, partiesRepo, supplierName, paymentMethod, items, status, expectedDelivery, notes): Long`. Shared by the purchase form and the scanner.

**Modified:** `PurchasesViewModel.recordPurchase` now delegates to `recordPurchaseFromDraft`; `gradle/libs.versions.toml` + `app/build.gradle.kts` (`com.google.mlkit:text-recognition:16.0.1`); `AndroidManifest.xml` + `res/xml/file_paths.xml` (FileProvider authority `${applicationId}.fileprovider`, added `receipts/` cache‑path; `shared/` already existed); `ui/AppViewModelProvider.kt` (register VM); `ui/PosApp.kt` (`receipt-scan` route); `ui/purchase/PurchaseScreen.kt` (`PurchaseListScreen` got an `onScanReceipt` param + a "Scan receipt" `SecBtn`).

**Tests:** `ReceiptParserTest.kt` (8 — incl. a real‑OCR multi‑column invoice fixture), `PurchaseRecorderTest.kt` (4, with in‑memory fakes).

### 4.1 Row‑grouping (`8ea8b05`)
ML Kit returns an item's **name** and **price** as *separate* fragments on wide receipts (left vs right column). `mergeRows` groups fragments that share a vertical band (via `OcrLine.bottom`) into one logical line before parsing, so the price rejoins its name. Verified live: a synthetic printed receipt auto‑extracted all items.

### 4.2 Column‑aware invoice parser (`b1da2c4`, PR #3)
Real Mauritian supplier invoices are formal multi‑column VAT invoices (**Code · Description · Qty · Unit Price · VAT · Total**). The simple parser grabbed the rightmost number (line **Total**) as the unit cost and polluted the name, and picked header noise ("CASHCHEQUE NO") as supplier. `parseTable()`:
- Detects the table by finding a header row containing Description + Quantity + a price label; uses the header cells' **x‑centres** as column anchors.
- Routes each body fragment to its **nearest column anchor**; pairs the Description / Quantity / Unit‑Price columns **top‑to‑bottom by index**.
- Maps **Description→name, Quantity→qty, Unit Price→unit cost (NOT Total)**. Footer cut off via a `FOOTER` regex (sub‑total/discount/…). Supplier from the letterhead via a `COMPANY` regex (prefers a line with "Ltd"/"Ltée"/hardware/…).
- Falls back to `parseSimple` when no such header exists (till receipts unaffected).
- **Unit‑tested with the real OCR fragments** captured from a *Hardwares Point Ltd* invoice.
- **Verified live on that real invoice:** supplier "Hardwares Point Ltd" ✓, all 6 items with correct names/qty/unit‑prices ✓, **Total Rs 15,270 == the invoice total** ✓.

> Note on the math (confirmed with the owner): per‑row **Total = Qty × Unit Price** (VAT‑inclusive); footer **grand Total = Subtotal + VAT**. NexaPOS uses **15% VAT‑inclusive** pricing, so storing the **Unit Price** (VAT‑inclusive) as cost is correct; the parser ignores the VAT/Total columns.
>
> **Known limit / future:** tuned for the Palladium‑style English layout. French headers (Désignation/Qté/Prix Unitaire/Montant/TVA) and invoices with only Qty+Total (no Unit Price) currently fall back to manual. Offered (not yet built): bilingual header synonyms + `Unit Price = Total ÷ Qty` fallback.

---

## 5. Feature — Styled PDF receipt share (PR #4, open)

The Sale Complete screen could only **send the receipt as text** (SMS/WhatsApp), and the "PDF" button rendered a plain monospace slip. Now the shared PDF is the **styled receipt slip** matching the on‑screen receipt, **with BRN + VAT**.

- `app/src/main/java/com/nexapos/retail/ui/checkout/ReceiptOutput.kt` — `sharePdf` → `renderPdf` builds a **content‑sized `PdfDocument` page with `Canvas`** (bold business name, `BRN … · VAT …` line, dashed separators, bold item names, right‑aligned amounts, bold TOTAL, footer). `html()` (used by `print()`) parameterized to take `widthCss`. A `ReceiptPaints` object holds the Paints; a `fit()` helper ellipsizes long names.
- `app/src/main/java/com/nexapos/retail/ui/checkout/PosReceiptScreen.kt` — added a **"Receipt PDF"** option to the *Send receipt* dialog (relabeled the text one "SMS (text)").
- BRN + VAT come from **Settings → Edit business profile** (`BusinessProfile.receiptLines()` prints `BRN x · VAT y`). They were blank in setup; once entered they print on every receipt/PDF.
- **Why not WebView→PDF:** `PrintDocumentAdapter.LayoutResultCallback`/`WriteResultCallback` have **package‑private constructors that neither Kotlin nor Java can subclass on compileSdk 35** — so HTML→PDF‑file is out; `PdfDocument` + `Canvas` is the reliable path. (`print()` still uses WebView→system‑print, which works because PrintManager owns the WebView.)
- **Verified live:** set BRN/VAT in Settings → made a sale (Cement Bag + Galvanized Screws + Glue = Rs 665) → tapped PDF → pulled `cache/shared/receipt-S-00012.pdf` and viewed it: clean styled slip with `BRN C20177445 · VAT VAT20188822`, correct items + totals.
- **Note:** SMS can't attach a PDF (text‑only). The PDF goes via the **share sheet** → WhatsApp / email / Bluetooth.

---

## 6. Feature — External (HID) barcode scanner — **DONE → PR #5**

> **Built, hardened & verified on-device.** Branch `feature/external-scanner` → **PR #5** (open). Spec `docs/superpowers/specs/2026-06-08-external-scanner-design.md`, plan `docs/superpowers/plans/2026-06-08-external-scanner.md`.
> **Files created:** `data/barcode/{BarcodeAssembler,ScannerBridge,ScannerEvents}.kt`, `data/profile/ScannerInput.kt`, `ui/settings/ScannerSettingsScreen.kt`, `test/.../BarcodeAssemblerTest.kt`. **Wired into:** `MainActivity.dispatchKeyEvent`, `ui/sale/PosSaleScreen.kt`, `ui/products/ProductsScreen.kt` (AddProduct), `ui/settings/SettingsScreen.kt` (groups), `ui/PosApp.kt` (`scanner-settings` route).
> **Final design (changed from the notes below after an adversarial review):** characters are **never** swallowed — only a *confirmed* scan's terminator is consumed; `RESET_GAP_MS = 50` so human typing never accumulates; `ScannerEvents` is a Kotlin `object` (not in `AppContainer`) and `tryEmit` is gated on `subscriptionCount > 0` (no stale replay).
> **Adversarial review (lily) caught + fixed 2 bugs:** speculative char-swallowing could eat fast typing/auto-repeat; buffered scans could replay to a later screen.
> **On-device verification:** tight key-burst on POS → miss-toast + search-fill, Enter swallowed (no stray nav); burst on Add Product → barcode field filled; PIN entry (slow keystrokes) unaffected; lone non-scan Enter not swallowed; Settings → Barcode scanner renders (toggle ON, terminator "Either").
>
> The subsections below are the original design notes (pre-implementation) — kept for reference.

### (original design notes)

A USB/Bluetooth scanner gun is an **HID keyboard** that "types" the barcode + Enter/Tab. We are mid‑**brainstorming** (`superpowers:brainstorming`). An exploration workflow produced a full integration map. **No code written yet** — the HARD GATE is design approval.

### Decided so far
- **Scope (user chose):** **POS till + register product barcodes.** POS: scan → add to cart (reuse `addByBarcode`). Add/Edit Product: scan → fill the `barcode` field. **Purchase is OUT of scope** (it has no barcode‑lookup today).

### Design presented (awaiting user "yes")
- **Approach:** global capture in **`MainActivity.dispatchKeyEvent`** (single Activity, before Compose) → a **`BarcodeAssembler`** (pure, unit‑tested) buffers chars with timing → on a **fast burst ending in Enter/Tab** (len ≥ 3, inter‑key gap < ~100 ms) emits the barcode to an app‑level **`ScannerEvents` `MutableSharedFlow<String>`** (replay 0, owned by `AppContainer`) → the **on‑screen collector** routes it (POS → `vm.addByBarcode`; Add/Edit Product → set `barcode`). Non‑bursts fall through to `super` so PIN/search/forms type normally.
- **Settings:** new `ScannerInput` prefs object (mirrors `ReceiptSettings`): `enabled` (default **ON**) + `terminator` (ENTER/TAB/**BOTH**). A Settings card with a toggle + terminator radio.
- **Defaults baked in:** terminator BOTH; require both fast‑burst AND terminator; burst chars swallowed so they don't pollute a focused field; unknown barcode on POS reuses the existing "No product matches X" toast + drops the code into search.

### Files (planned — not created)
**Create:** `data/barcode/BarcodeAssembler.kt` (pure, unit‑tested), `data/barcode/ScannerBridge.kt` (KeyEvent→char/ts adapter), `data/barcode/ScannerEvents.kt` (SharedFlow bus), `data/profile/ScannerInput.kt` (prefs).
**Modify:** `MainActivity.kt` (`dispatchKeyEvent` override — currently **no key handling**, lines ~14‑29), `di/AppContainer.kt` (own `ScannerEvents`), `ui/sale/PosSaleScreen.kt` (collector → `addByBarcode`; existing camera path at `PosSaleScreen.kt:150‑163`, VM `SellingViewModel.addByBarcode` ~214‑223), `ui/products/ProductsScreen.kt` (collector → `barcode = code`; camera callback at `ProductsScreen.kt:748`), `ui/settings/SettingsScreen.kt` (scanner card), maybe `ui/PosApp.kt` (route if Settings is a sub‑screen).
**No change** to `BarcodeScanner.kt` (camera), DB, or `Product` entity.

### Next steps for this feature
1. **Get the user to approve the design** (or revise).
2. Write spec → `docs/superpowers/specs/2026-06-08-external-scanner-design.md`, commit.
3. `superpowers:writing-plans` → implementation plan.
4. Implement (probably on a new branch `feature/external-scanner` off `main`); unit‑test `BarcodeAssembler`; verify on emulator.

Tracking tasks: **#19** Ext scanner: brainstorm & design (in_progress) · **#20** write+review spec · **#21** plan+build+verify.

---

## 7. Outstanding / follow‑ups

- **External scanner**: resume at §6 (approve design → spec → plan → build).
- **PRs #3 and #4** are open — user to review/merge (independent; any order).
- **Two minor receipt‑scan polish items** (noted, not done): Register button gives no feedback if supplier/lines empty; review‑screen thumbnail decodes on the main thread.
- **Column‑aware parser future**: bilingual (French) headers + `Unit Price = Total ÷ Qty` fallback (offered, not built).
- `.github/` (CI workflow) still untracked — the GitHub token lacks `workflow` scope; add via web UI.
- `docs/SESSION_LOG_v1.0.0.md` and this file are untracked working logs.

---

## 8. Environment & command reference

- **JDK:** `JAVA_HOME = D:\Android\jdk17` (JDK 17).
- **Build:** `./gradlew assembleDebug` · **Checks:** `./gradlew testDebugUnitTest detekt ktlintCheck --continue` · **Format:** `./gradlew ktlintFormat` **in its own invocation first**.
- **SDK / adb:** `/d/Android/Sdk/platform-tools/adb.exe` · **Emulator:** `/d/Android/Sdk/emulator/emulator.exe`.
- **AVD:** `Galaxy_Tab_A11` (1340×800 @ 179 dpi, landscape). Launch **detached** so it survives: PowerShell `Start-Process -FilePath "D:\Android\Sdk\emulator\emulator.exe" -ArgumentList '-avd','Galaxy_Tab_A11',...`.
- **Staff PIN:** `4827`. **Business:** QUINCAILLERIE RB TRADING, Royal Rd Curepipe; BRN `C20177445`, VAT `VAT20188822` (set in Settings → Edit).
- **Signing (gitignored):** `keystore.properties`, `nexapos-release.jks` (alias `nexapos`, pwd `NexaPOS-RB-2026`).

### Gotchas learned this session
- **Emulator idle‑reaps** — relaunch detached via `Start-Process` (a backgrounded `emulator` task gets killed when the task is reaped, saving a `default_boot` snapshot).
- **PIN soft‑keyboard** sometimes covers the Sign‑in button: type PIN → `adb shell input keyevent 4` (hide keyboard) → tap Sign in.
- **Photo picker / DocumentsUI** doesn't show adb‑pushed images until a media scan: `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/x.png`, or use **Browse → device storage → Pictures**. Use `MSYS_NO_PATHCONV=1` for `/sdcard` paths in Git Bash.
- **`adb shell run-as <pkg>` can't write `shared_prefs`** via stdin redirect (SELinux/permission) — set business profile via the **Settings UI** instead. (Reading via `run-as ... cat` works.)
- **`PrintDocumentAdapter.LayoutResultCallback`/`WriteResultCallback`** have package‑private constructors → not subclassable (Kotlin or Java) on compileSdk 35 → use `PdfDocument`+`Canvas`, not WebView→PDF‑file.
- **detekt** patterns used: data‑driven table to drop `CyclomaticComplexMethod`; `@Suppress("LoopWithTooManyJumpStatements" / "SwallowedException" / "TooGenericExceptionCaught" / "LongMethod")` with a justifying comment; empty fake overrides as `= Unit` (not `{}`) for `EmptyFunctionBlock`.
- **Incremental Kotlin builds can serve stale classes** after branch ops — when behaviour contradicts source, try a clean rebuild (though the real CSV bug above was a genuine source bug, not staleness).
