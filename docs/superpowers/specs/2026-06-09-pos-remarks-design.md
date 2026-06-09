# POS Remarks (Pillar B2) — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/pos-remarks` (off `main`). **DB v10 → v11** (additive, non-destructive).

> Pillar B2 of the customizable-POS request — completes Pillar B (B1 = action bar, shipped). A Remarks button on the POS action bar; the note is saved on the sale and printed on the receipt.

## Purpose
Let the cashier attach a free-text note to a sale (delivery instructions, customer ref). The note is stored on the sale record and printed on every receipt surface.

## Data + migration
- `Sale` gains `note: String = ""` with `@ColumnInfo(defaultValue = "")` so Room's expected schema matches the `ALTER … DEFAULT ''` migration (else a destructive rebuild).
- DB **v10 → v11**: `MIGRATION_10_11 = ALTER TABLE sales ADD COLUMN note TEXT NOT NULL DEFAULT ''`. Bump `version = 11`, register in `AppContainer.addMigrations(…, MIGRATION_10_11)`, commit the exported `11.json`.

## ViewModel (`SellingViewModel`)
- New state `var saleNote by mutableStateOf("")`.
- `SaleSnapshot` gains `note: String = ""`; the snapshot built in `complete()` sets `note = saleNote.trim()`.
- `persist()` writes `Sale(note = saleNote.trim(), …)`.
- `saleNote` resets to `""` in `startNewTicket()` (fresh ticket) and `clearCart()` (Void) — it belongs to the current ticket. It **survives `beginCheckout()`** so it persists with the sale.

## UI (`PosSaleScreen`)
- A **Remarks** button on the action bar's second row (replaces the current empty `Spacer` slot beside Hold / Void / Exit). Label shows `Remarks •` when `saleNote` is non-blank.
- **Remarks dialog**: an `AlertDialog` with a multi-line `BasicTextField` bound to `vm.saleNote` (placeholder "Delivery instructions, customer ref…"); a **Done** button closes it; a **Clear** button empties the note.

## Receipt (`PosReceiptScreen` + `ReceiptOutput`)
- When `sale.note` is non-blank, render a **"Note: <note>"** line: on the on-screen receipt (under the items / totals block), the thermal/printed receipt, and the PDF/HTML.

## Testing
- **Unit** (`SellingViewModelTest`): a completed sale with `saleNote` set persists `Sale.note`; the `SaleSnapshot.note` carries it.
- **Migration**: `11.json` exported and matches the entity; a fresh DB at v11 + the migration path both yield the `note` column (the standard schema check we run each migration).
- **On-device**: type a remark → it prints on the receipt and is stored with the sale.

## Files
**Modify:** `data/entity/Sale.kt`, `data/PosDatabase.kt` (v11 + MIGRATION_10_11), `di/AppContainer.kt`, `ui/sale/SellingViewModel.kt` (saleNote + SaleSnapshot.note + persist + reset), `ui/sale/PosSaleScreen.kt` (Remarks button + dialog), `ui/checkout/PosReceiptScreen.kt` + `ui/checkout/ReceiptOutput.kt` (Note line). **Add:** `app/schemas/com.nexapos.retail.data.PosDatabase/11.json`.

## Out of scope
Per-line notes; editing a note after the sale is completed; note search/filter in sales history.
