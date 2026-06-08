# External (HID) Barcode Scanner — Design Spec

**Date:** 2026-06-08 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/external-scanner` (off `main`).

## Purpose

Let the shop use a physical **USB or Bluetooth barcode scanner gun** at the counter. Such scanners present to the OS as an **HID keyboard**: when a barcode is scanned they "type" the barcode characters very fast and end with a terminator key (Enter or Tab). NexaPOS today only has a **camera** scanner (`BarcodeScanner` via play-services-code-scanner); this adds hardware-scanner support without touching the camera path.

## Scope (agreed)

In scope — a completed scan acts on whichever screen is open:
- **POS Sale** → add the matching product to the current ticket (reuse `SellingViewModel.addByBarcode`).
- **Add/Edit Product** → fill the product's `barcode` field (so the owner registers a barcode by scanning it).

Out of scope (this iteration):
- **Purchase** screens (no product-by-barcode lookup exists there today).
- Multi-symbology configuration, scanner-vendor SDKs (DataWedge etc.), camera-path changes, DB/schema changes.

## Architecture

An HID scanner's keystrokes reach the single host **`MainActivity`** before Jetpack Compose. We intercept them there, assemble the burst into a barcode, and publish the finished code on an app-level event flow that the on-screen Compose collector handles.

```
HID scanner types "5901234..⏎" (fast)
        │  hardware KeyEvents
        ▼
MainActivity.dispatchKeyEvent ──► ScannerBridge ──► BarcodeAssembler (pure, timing + terminator)
        │                                                   │ emits "5901234123457" on terminator
        │ (non-burst keys fall through to super → normal typing)
        ▼
ScannerEvents (MutableSharedFlow<String>, replay 0)  ◄── emit()
        ▲ collect() in a LaunchedEffect on the active screen
        ├── PosSaleScreen   → vm.addByBarcode(code)  (+ existing miss-toast/search fallback)
        └── AddProductForm  → barcode = code
```

**Why a `SharedFlow` (not StateFlow / CompositionLocal):** the producer lives in the Activity, outside the composition; we need a **one-shot event**, not retained state. `SharedFlow(replay = 0)` delivers each scan exactly once to the currently-composed collector. A `StateFlow` would re-deliver the last code on recomposition (double add-to-cart); a `CompositionLocal` can't be written from `dispatchKeyEvent`. The codebase has no existing event bus, so we add one.

## Components

### New (`data/barcode/`)
- **`BarcodeAssembler`** — *pure Kotlin, unit-tested.* `feed(char: Char, atMs: Long): String?` accumulates characters; resets the buffer when the inter-key gap exceeds `GAP_RESET_MS`; on a terminator call (`finish(atMs)`) returns the buffer iff it was built as a fast burst of length ≥ `MIN_LEN`, else null. Holds the tuning constants. No Android types → fully unit-testable (mirrors the `ReceiptParser` pure pattern).
- **`ScannerBridge`** — thin adapter held by `MainActivity`. Translates `KeyEvent`s into `(char, timestamp)` for `BarcodeAssembler` (using `event.unicodeChar`, honoring meta state), recognizes the configured terminator key(s), and returns `true` only for the terminator of a recognized scan (so only the completed scan is "swallowed"). Reads `ScannerInput` for the enabled flag + terminator.
- **`ScannerEvents`** — app-level `MutableSharedFlow<String>` (replay 0, `DROP_OLDEST` buffer) exposing `val scans` + `fun tryEmit(code)`, implemented as a Kotlin `object` singleton (matching `BarcodeScanner`/`ReceiptSettings`). `tryEmit` only emits when `subscriptionCount > 0`, so a scan with no screen listening is dropped, never queued for a later screen.

### New (`data/profile/`)
- **`ScannerInput`** — `SharedPreferences` settings object mirroring `ReceiptSettings`: `enabled(context): Boolean` (default **true**), `terminator(context): Terminator` enum `ENTER`/`TAB`/`BOTH` (default **BOTH**), with `set…` writers.

### Modified
- **`MainActivity`** — override `dispatchKeyEvent(event)`: if `ScannerInput.enabled` and `scannerBridge.feed(event)` consumed a completed scan → `return true`; else `return super.dispatchKeyEvent(event)`. Hold one `ScannerBridge` that emits to the container's `ScannerEvents`.
- (`ScannerEvents` is a top-level `object`, so no DI wiring is needed — `MainActivity` and the screens reference it directly.)
- **`PosSaleScreen`** — `LaunchedEffect` collecting `ScannerEvents.scans`; for each code call the existing `vm.addByBarcode(code)` (same path as the camera button, including the not-found toast + drop-into-search behavior).
- **`AddProduct`/`ProductsScreen` form** — `LaunchedEffect` collecting `ScannerEvents.scans` → set the `barcode` field (same effect as the existing camera callback).
- **`SettingsScreen`** — add an "External barcode scanner" card: a toggle (enabled) + a 3-way terminator selector, reusing existing `Card`/`RadioRow` components. (May be inline or a small sub-screen registered in `PosApp`.)

**No change** to `BarcodeScanner.kt` (camera), `Product` entity, DAOs, or repositories — the hardware path reuses the existing in-memory `addByBarcode` lookup and the existing unique `barcode` index.

## Distinguishing a scan from human typing

Two independent signals must both hold (computed in `BarcodeAssembler`):
1. **Speed** — consecutive characters arrive under `RESET_GAP_MS` (50 ms). A human keystroke gap exceeds this and resets the buffer, so manual typing never accumulates into a scan.
2. **Terminator + minimum length** — the burst ends in Enter/Tab and the buffer length ≥ `MIN_LEN` (3). A lone Enter (e.g. a login submit) has too short a buffer → not a scan → passes through.

Characters are **never** swallowed — every keystroke reaches the focused field, so the PIN pad, search boxes, forms, and key auto-repeat keep working unchanged. Only the **terminator** of a confirmed scan is consumed (so the scanner's Enter doesn't leak into the app). The scanned digits may briefly appear in a focused field — cosmetic; on POS the search box is cleared on a successful add.

## Error / edge handling
- **Feature disabled** → `dispatchKeyEvent` short-circuits to `super`; zero behavior change.
- **Unknown barcode on POS** → reuse the existing `addByBarcode` miss handling (toast "No product matches …" + put the code in the search box).
- **Scan with no relevant screen open** (e.g. on the Home dashboard) → `tryEmit` is gated on `subscriptionCount > 0`, so the event is dropped, never queued for the next screen. No crash.
- **Pref read cost** → `SharedPreferences` is in-memory after first read; reading per key-event is cheap.

## Testing
- **Unit tests** (`BarcodeAssemblerTest`): fast burst + Enter → emits code; fast burst + Tab → emits when terminator allows; slow "typing" + Enter → null; burst shorter than `MIN_LEN` → null; gap mid-burst resets; terminator-only → null.
- **Manual on emulator/device**: a real/simulated HID scan on POS adds the product to the cart; on Add/Edit Product fills the barcode field; typing the PIN and searching still work normally; the Settings toggle disables capture.

## Files
**Create:** `data/barcode/BarcodeAssembler.kt`, `data/barcode/ScannerBridge.kt`, `data/barcode/ScannerEvents.kt`, `data/profile/ScannerInput.kt`, `test/.../BarcodeAssemblerTest.kt`.
**Modify:** `MainActivity.kt`, `di/AppContainer.kt`, `ui/sale/PosSaleScreen.kt`, `ui/products/ProductsScreen.kt`, `ui/settings/SettingsScreen.kt` (+ `ui/PosApp.kt` only if Settings becomes a sub-screen).

## Tuning defaults
`RESET_GAP_MS = 50`, `MIN_LEN = 3`, terminator = `BOTH` (Enter or Tab). Characters are never swallowed; only a confirmed scan's terminator is consumed. All adjustable; terminator is user-configurable in Settings.
