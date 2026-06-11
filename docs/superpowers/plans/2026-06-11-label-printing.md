# Thermal Barcode-Label Printing — Implementation Plan

**Spec:** `docs/superpowers/specs/2026-06-11-label-printing-design.md`
**Branch:** `feature/label-printing` off `main`.
**Per-stage gate:** `ktlintFormat` (own invocation) → `assembleDebug` +
`testDebugUnitTest` + `detekt` + `ktlintCheck` green → one commit.
Physical-print verification is owner-side (no printer on the emulator); each
stage below states what CAN be machine-verified.

---

## Stage 1 — TSPL protocol core (pure JVM)

- `data/hardware/labels/Tspl.kt` — pure command builder, mirrors `EscPos.kt`:
  `setup(widthMm, heightMm, gapMm)` (SIZE/GAP/DIRECTION/CLS), `text(x, y, font,
  scale, content)` with quote-escaping, `barcode(x, y, value)` choosing
  `EAN13` (via `Ean13.isValid`) vs `128`, `print(copies)`, and a
  `label(spec): ByteArray` composing one full label. mm→dot math at 8 dots/mm.
- `domain/hardware/LabelPrinter.kt` — `LabelSpec(name, sku, barcode,
  priceCentsOrNull, copies)`, `interface LabelPrinter { suspend fun
  print(labels, onProgress): PrintOutcome }` where `PrintOutcome` is
  Done / FailedAt(index, reason).
- **Tests:** `TsplTest` — exact command strings for a known label, escaping,
  EAN13-vs-128 pick, copies, mm→dot rounding, multi-line name split.

## Stage 2 — Transport session + printer impl + settings

- `data/hardware/labels/LabelSession.kt` — `open() / write(bytes) / close()`
  over Bluetooth SPP or TCP 9100 (reuse the drawer transports' socket code;
  drawer files stay untouched).
- `TsplLabelPrinter : LabelPrinter` — one session per batch, per-item write,
  progress callback, FailedAt on IOException.
- `data/profile/LabelPrinterSettings.kt` — mirrors `DrawerSettings`:
  transport, BT mac/name, LAN host/port (default 9100), size preset/custom,
  show-price toggle.
- `ui/settings/LabelPrinterSettingsScreen.kt` — mirrors the drawer screen:
  transport picker, paired-BT list, LAN fields, size preset, **Print test
  label**, plus a debug **Share TSPL as file** action (verification without
  hardware). Route `label-printer-settings`, Settings → “Receipt & hardware”.
- `AppContainer`: lazy `labelPrinter`.
- **Tests:** LAN session against an in-process fake TCP server (the
  `LanDrawerTransportTest` pattern): bytes arrive in order, FailedAt index
  correct when the server drops mid-batch.

## Stage 3 — Label printing module (UI)

- `ui/labels/LabelPrintViewModel.kt` — catalog flow, search/category/in-stock
  filters, per-item copies map, bulk “1 each” / “= stock qty” / clear,
  missing-barcode bulk **Generate & save** (existing `Ean13` in-store
  generator + `CatalogRepository.upsert`), `print()` with progress state and
  resume-from-index retry.
- `ui/labels/LabelPrintScreen.kt` — list w/ steppers, totals bar
  (“14 products · 162 labels”), missing-barcode banner, print progress +
  cancel + retry, secondary “Print on A4 sheet” fallback (existing
  `printProductLabels`).
- Nav: route `labels`; Products toolbar button + dashboard **Print labels**
  quick action point here.
- **Tests:** ViewModel with `FakeLabelPrinter` — copies bookkeeping, bulk
  actions, skip-no-barcode, generate-missing persists, resume retries from
  the failed index, cancel stops cleanly.
- Docs: SCREENS.md + ARCHITECTURE.md one-liners.

---

## Owner actions
1. Confirm the printer model speaks **TSPL** before/while Stage 2 (XPrinter,
   TSC, HPRT, Gprinter, iDPRT all do).
2. Physical test at Stage 2 (test label) and Stage 3 (a real 20–50 item batch,
   checking scanner reads the printed EAN-13s).

## Token discipline
No agent fan-out for the build stages; one optional focused review pass at the
end if requested. Everything testable is JVM-unit-tested; emulator use is
limited to screen smoke checks.
