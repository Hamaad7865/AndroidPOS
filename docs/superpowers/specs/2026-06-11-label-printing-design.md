# Thermal Barcode-Label Printing — Design Spec

**Date:** 2026-06-11 · **Status:** draft, awaiting owner approval.
**Branch:** `feature/label-printing` (off `main`).

## Purpose

A shop receives stock (say 1 000 items) and must stick a barcode label on every
unit. This adds a **Label printing module**: pick products, set **how many
labels per item**, and print **Name + SKU + barcode** labels on a cheap
**thermal label printer** (Bluetooth or LAN). The existing A4 sticker-sheet
path (`printProductLabels`, system print dialog) stays as the no-hardware
fallback.

## Architecture decision

Follow the repo's proven **hardware-feature template** (the cash-drawer stack),
not a new gradle module: this codebase is single-module with manual DI
(`AppContainer`) by design; the NowInAndroid principles we keep are the ones
that matter — interface + fake for testability, pure protocol builders, UDF
ViewModel, offline-first.

| Layer | Drawer (existing precedent) | Labels (new) |
|---|---|---|
| Pure protocol | `EscPos.kt` (JVM-testable bytes) | `Tspl.kt` (JVM-testable commands) |
| Transport | `BluetoothDrawerTransport` / `LanDrawerTransport` | `LabelSession` reusing the same BT-SPP / TCP-9100 socket code, but **one connection per batch** (a kick is one-shot; 1 000 labels must stream over a single session) |
| Domain interface | `DrawerKicker` | `LabelPrinter` (+ `FakeLabelPrinter` for VM tests) |
| Settings | `DrawerSettings` + screen | `LabelPrinterSettings` + screen (mirrors it) |
| UI | — | `ui/labels/` screen + ViewModel |

**Protocol = TSPL** (Taiwan Semiconductor Printer Language). The affordable
label printers this market actually buys — XPrinter XP-420B/365B, TSC, HPRT,
Gprinter, iDPRT — all speak TSPL over Bluetooth SPP and TCP 9100. It is plain
ASCII (`SIZE`, `GAP`, `CLS`, `TEXT`, `BARCODE`, `PRINT m,n`), has **native
barcode rendering** (crisp at printer resolution, ~100 bytes per label instead
of shipping bitmaps over Bluetooth), and **native copies** (`PRINT 1,n` = our
per-item count, free). ESC/POS receipt printers can't feed gapped label stock;
ZPL (Zebra) and vendor SDKs are out of scope v1. The protocol sits behind
`LabelPrinter`, so a ZPL impl can be added later without touching the UI.

> **Hardware assumption to confirm with the owner before Stage 2:** the target
> printer speaks TSPL (true of the cheap standard models). If a specific model
> is already bought, check its manual first.

## Label layout (defaults)

203 dpi ⇒ 8 dots/mm. Size presets: **40×30 mm** (default), 50×30, 58×40,
custom. Content top→bottom, centred:

1. **Name** — up to 2 lines, ellipsised (TSPL `TEXT`, built-in font scaled).
2. **SKU** — one line (omitted if blank).
3. **Barcode** — auto symbology: valid 13-digit EAN ⇒ `EAN13`, anything else ⇒
   `128` (Code128 accepts free text). Human-readable digits on.
4. **Price** — optional toggle in settings (default **off** — spec says
   Name + SKU + barcode), printed `Rs 1,234.56` via `Money.format`.

## The module (`ui/labels/`)

- Product list reusing catalog data (search, category filter, in-stock filter).
- **Per-item copies stepper** (0 = skip). Bulk actions: **“1 each”**,
  **“= stock qty”** (the receiving-day killer: one label per physical unit),
  **“Clear”**. Running total: “14 products · 162 labels”.
- **Missing barcodes:** banner “N selected items have no barcode” with
  **Generate & save** (bulk, confirm dialog) using the existing in-store
  `Ean13` generator (prefix 200…) via `CatalogRepository.upsert` — same code
  path as the product form's Generate button. Items left without a barcode
  print Name+SKU only? No — they are **skipped** with a count, to avoid
  unscannable labels.
- **Print:** one socket session; per item send `CLS … PRINT 1,copies`;
  progress “item i of n”; **Cancel** between items; on socket failure show
  “stopped at item i — Retry from here” (resume index kept in the VM).
- Entry points: Products screen toolbar + the dashboard **Print labels** quick
  action route here; the old A4 sheet becomes a secondary “Print on A4 sheet”
  button for shops without a label printer.

## Settings (`LabelPrinterSettings` + screen, mirrors drawer)

Transport (Bluetooth paired-device picker / LAN host+port 9100), label size
preset (+ custom W×H mm, gap mm), print density (printer default), show-price
toggle, **Print test label**.

## Failure modes

| Situation | Behaviour |
|---|---|
| Printer unreachable | Pre-flight connect fails fast → toast + nothing printed |
| Socket drops mid-batch | Stop, keep resume index, offer “Retry from item i” |
| Item without barcode | Skipped, counted in the summary banner |
| Paper-out / cover-open | Not detectable reliably on cheap printers over SPP — out of scope v1, documented |

## Out of scope (v1)

ZPL/Zebra, vendor SDK AARs, USB-OTG transport, label images/logos, paper-out
status polling, printing from the receipt printer, label template designer.

## Testing

`Tspl.kt` fully unit-tested (mm→dot math, escaping, symbology pick, copies).
LAN session tested against an in-process fake TCP server (the
`LanDrawerTransportTest` precedent). ViewModel tested with `FakeLabelPrinter`
(records jobs; injectable failure at item k for resume logic). Physical print
verification is owner-side — the emulator has no printer; Stage 2 ships a
“share TSPL bytes as file” debug action so output can be eyeballed without
hardware.
