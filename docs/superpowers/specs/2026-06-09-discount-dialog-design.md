# Discount Dialog (cart + item) — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/pos-discount` (stacked on `feature/vat-types` / PR #7). **Supersedes** the inline-toggle design (`2026-06-08-pos-discount-design.md`); the cart-discount VM logic (`applyDiscount`, `percentToFlat`, `flatToPercent`) is reused.

## Purpose
Replace the inline checkout discount field with a **discount dialog** matching the owner's reference POS: tabs for **Cart discount** and **Item discount**, each with a `% / Rs` entry, a **Subtotal / Total discount / Total** summary, and **Clear · OK · Cancel**. Cart discount applies to the whole ticket; item discounts apply per line.

## Data model
- **`PosLine`** (in-memory) gains `discount: Int = 0` (flat Rs per line; the source of truth — `%` is only the dialog's input mode). Add `net get() = (lineTotal - discount).coerceAtLeast(0)`.
- **`SaleItem`** (persisted) gains `@ColumnInfo(defaultValue = "0") val discountCents: Long = 0`. **Migration v7→v8** (non-destructive): `ALTER TABLE sale_items ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0`; register via `.addMigrations(MIGRATION_7_8)`; commit the exported `8.json`; migration-tested.
- Cart discount stays `vm.discount` + `discountIsPercent` / `discountPercent` (already built) — but the `%` now computes on the **after-item** subtotal.

## Calc (`SellingViewModel`)
- `subtotal` = Σ `lineTotal` (pre-discount; unchanged).
- `itemDiscountTotal` = Σ `line.discount`.
- `afterItems` = `(subtotal − itemDiscountTotal).coerceAtLeast(0)`.
- `clampedDiscount` (cart) = `discount.coerceIn(0, afterItems)`.
- `applyDiscount` percent branch now computes `discount = percentToFlat(afterItems, discountPercent)` (after-item base).
- `totalDiscount` = `itemDiscountTotal + clampedDiscount`.
- `total` = `(afterItems − clampedDiscount + shipping).coerceAtLeast(0)`.
- **`vat`** = pure `discountedVat(lines, cartDiscount, vatRegistered)`:
  - `0` if not registered;
  - else `standardNet` = Σ over **STANDARD** lines of `line.net`; `afterItems` = Σ `line.net`; `standardFinal` = `if (afterItems > 0) standardNet * (afterItems − cartDiscount).toDouble() / afterItems else 0.0`; return `(standardFinal − standardFinal / 1.15).roundToInt()`.
  - i.e. VAT is the 15%-inclusive portion of the standard-rated amount **after item discounts and the proportional share of the cart discount**. (`cartDiscount` passed = `clampedDiscount`.)
- **`applyItemDiscount(productId: String, isPercent: Boolean, value: Int)`**: compute `flat = if (isPercent) percentToFlat(line.lineTotal, value) else value.coerceAtLeast(0)`, clamp to `line.lineTotal`; replace the matching line via `workingLines[i] = workingLines[i].copy(discount = flat)`; re-sync tender (`if (!isCredit) received = total`).
- **`clearAllDiscounts()`**: cart `discount/discountIsPercent/discountPercent` = 0/false/0 and every `line.discount` = 0; re-sync tender.
- `startNewTicket()` and `beginCheckout()` clear line discounts too (they already reset the cart-discount state).

## Persist
- `persist()`: `SaleItem.discountCents = line.discount * CENTS_PER_RUPEE`; `unitPriceCents`/`lineTotalCents` unchanged (gross). `SaleSnapshot.lines` carry the per-line discounts (they already hold `PosLine`). `Sale.discountCents` stays the **cart** discount (`snapshot.discount` = clamped cart discount). Item discounts live on `SaleItem`s; the total discount is derivable (cart + Σ item).

## UI (`PosCheckoutScreen`)
- The charges area's inline `DiscountField` becomes a **"Discount" row/button** showing the current total discount (`− Rs {totalDiscount}`); tapping it opens `DiscountDialog`. (`Shipping` keeps its `NumField`.)
- **`DiscountDialog`** (modal):
  - On open, snapshot the current discounts (for Cancel/revert).
  - Two tabs: **Cart discount** | **Item discount**.
    - *Cart*: `% / Rs` toggle (reuse `ModeChip`), a value field (bound to `applyDiscount`), and a **Subtotal / Total discount / Total** summary.
    - *Item*: the cart lines; each row shows name · `lineTotal` · its current discount; tapping a row reveals a small `% / Rs` value entry → `applyItemDiscount(productId, …)`.
  - Footer: **Clear** (`clearAllDiscounts`), **Cancel** (restore the snapshot), **OK** (dismiss, keep).
- The checkout breakdown shows `Subtotal`, `Discount` (= `totalDiscount`), `VAT` (gated by registration), `Total`.

## Receipt
- Each discounted line shows its discount beneath it (e.g. `− Rs X`); the summary's `Discount` row shows the **total** discount. Applies to `PosReceiptScreen` (on-screen), `ReceiptOutput.html` (print) + `renderPdf` (PDF) + `messageText` (SMS/WhatsApp).

## Migration safety & test
- Use `@ColumnInfo(defaultValue = "0")` so Room's expected schema matches the migration (the VAT-feature lesson); commit `8.json`.
- **On-device v7→v8 test:** install the new build over the current v7 DB (TESTSHOP + product) and confirm the data survives; plus inspect `8.json` for `discountCents … DEFAULT 0`.

## Testing
- **Unit** (`DiscountCalcTest` / `VatCalcTest`): `discountedVat` — no discount equals the old VAT; an item discount lowers VAT; a cart discount lowers VAT; mixed VAT types with a cart discount distribute proportionally; not-registered → 0. Cart `%` computed on `afterItems`. (`percentToFlat`/`flatToPercent` already covered.)
- **On-device**: cart discount (% and Rs); item discount on a line; both together; Clear; Cancel reverts; OK keeps; receipt shows per-line + total discounts; migration preserves data.

## Files
**Create:** `DiscountDialog` composable (in `PosCheckoutScreen.kt` or a sibling file); `discountedVat` (in `VatCalc.kt`) + tests; `MIGRATION_7_8` (in `PosDatabase.kt`).
**Modify:** `data/entity/SaleItem.kt`, `data/PosDatabase.kt` (v8 + migration), `di/AppContainer.kt`, `ui/sale/SellingViewModel.kt` (PosLine.discount + calc + `applyItemDiscount` + `clearAllDiscounts` + persist + `beginCheckout`/`startNewTicket`), `ui/sale/VatCalc.kt`, `ui/checkout/PosCheckoutScreen.kt` (Discount row + `DiscountDialog`; remove the inline `DiscountField`/`ModeChip`), `ui/checkout/PosReceiptScreen.kt`, `ui/checkout/ReceiptOutput.kt`.

## Out of scope
Discounting shipping; persisting the chosen `%` (only the flat Rs is stored); discount reasons / approval codes; item discounts on the live POS ticket (set at checkout only).
