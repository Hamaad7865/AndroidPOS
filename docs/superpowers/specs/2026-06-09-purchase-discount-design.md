# Purchase Supplier Discount — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/purchase-discount` (off `main`). **DB v9 → v10.**

## Purpose
Record a **supplier discount** on a purchase order. The discount (entered as `%` or `Rs`) applies to the whole order: **`Total = Subtotal − Discount`**.

## Model
- **`Purchase`** gains `@ColumnInfo(defaultValue = "0") val discountCents: Long = 0`. **Migration v9→v10** (non-destructive): `ALTER TABLE purchases ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0`; register via `.addMigrations(…, MIGRATION_9_10)`; commit `10.json`; migration-tested.
- `Purchase.totalCents` becomes **net** (`subtotal − discount`); `discountCents` stores the flat discount. The `%` is only an input mode (not persisted), mirroring the sale discount.

## Recorder (`recordPurchaseFromDraft`)
- New trailing param `discountRupees: Int = 0` (the flat discount; any `%` is converted in the form before the call).
- `subtotal = lines.sumOf { it.lineTotalCents }`; `discountCents = (discountRupees * CENTS_PER_RUPEE).coerceIn(0, subtotal)`; `totalCents = subtotal − discountCents`; persist `discountCents` on the `Purchase`.

## Form (`PurchaseForms` "New Purchase Order" + `PurchasesViewModel`)
- A discount control between **Subtotal** and **Total**: a **`% / Rs` toggle + value field**, reusing **`percentToFlat`** (`com.nexapos.retail.ui.sale.DiscountCalc`).
- State (held by the form/VM): `discountIsPercent: Boolean`, `discountValue: Int`. `discountFlat = if (isPercent) percentToFlat(subtotal, discountValue) else discountValue`, clamped to `0..subtotal`. `Total = subtotal − discountFlat`.
- On **Confirm** → `recordPurchaseFromDraft(…, discountRupees = discountFlat)`; reset the discount state on success.

## Detail / list
- `DetailTotalsCard` shows **Subtotal · Discount (when > 0) · Total** (`Subtotal` = Σ line totals, `Discount` = `purchase.discountCents`, `Total` = `purchase.totalCents`).
- The PO list total already reads `purchase.totalCents` (now net) — no change.

## Receipt-scan path (`ReceiptScanViewModel`)
- Calls `recordPurchaseFromDraft` with the default `discountRupees = 0` — no behaviour change (the new param defaults).

## Migration safety & test
- `@ColumnInfo(defaultValue = "0")` so Room's expected schema matches the migration; commit `10.json`. On-device v9→v10: install over the current v9 DB and confirm existing purchases/products survive.

## Testing
- **Unit** (recorder test): a purchase with a flat discount → `totalCents == subtotal − discount`, `discountCents` stored; a discount larger than subtotal is clamped to subtotal (total = 0); `discountRupees = 0` is unchanged behaviour.
- **On-device**: v9→v10 migration preserves data; create a PO, apply a 10% discount → Total is net; the PO detail shows the discount line.

## Files
**Create:** `MIGRATION_9_10` (in `PosDatabase.kt`).
**Modify:** `data/entity/Purchase.kt`, `data/PosDatabase.kt` (v10 + migration), `di/AppContainer.kt`, `ui/purchase/PurchaseRecorder.kt`, `ui/purchase/PurchasesViewModel.kt`, `ui/purchase/PurchaseForms.kt`; recorder unit test.

## Out of scope
Per-line purchase discounts; a discount on the scanned-receipt flow; changing per-unit product costs.
