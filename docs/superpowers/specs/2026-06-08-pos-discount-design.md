# POS Discount — % or Rs with quick presets — Design Spec

**Date:** 2026-06-08 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/pos-discount` (stacked on `feature/vat-types` / PR #7 — merge that first; this branch shares `SellingViewModel`/`PosCheckoutScreen`/`PosSaleScreen` with it).

## Purpose
Make the checkout discount **minimal-interaction**: a **% / Rs** toggle plus one-tap **preset chips** (5% / 10% / 15% / Clear). The stored discount stays a **flat Rs** amount — the percentage is only an input mode that computes into it.

## Model (no DB change)
- `SellingViewModel.discount` stays flat **whole rupees** and continues to drive `total = subtotal − clampedDiscount + shipping` (`clampedDiscount = discount.coerceIn(0, subtotal)`).
- Add VM state: `discountIsPercent: Boolean` (default **false** = Rs, preserving today's behaviour) and `discountPercent: Int` (0).
- `applyDiscount(isPercent: Boolean, value: Int)`:
  - sets `discountIsPercent = isPercent`;
  - if percent → `discountPercent = value.coerceIn(0, 100)`, `discount = percentToFlat(subtotal, discountPercent)`;
  - if flat → `discount = value.coerceAtLeast(0)` (and leaves `discountPercent` as the last percent for display);
  - re-syncs the tender exactly like the current field: `if (!isCredit) received = total`.
- Pure helper **`percentToFlat(subtotal: Int, pct: Int): Int = (subtotal * pct.coerceIn(0, 100) / 100.0).roundToInt()`** — **unit-tested**.
- `startNewTicket()` also resets `discountIsPercent = false`, `discountPercent = 0` (alongside the existing `discount = 0`).

## UI (`PosCheckoutScreen`, "charges" Row)
Replace the single `NumField("Discount (flat Rs)", …)` with a **`DiscountField`** composable (the Shipping `NumField` stays unchanged beside/below it):
- A compact **`[ % | Rs ]` segmented toggle** → `vm.applyDiscount(isPercent = …, value = currentInput)` on switch (re-interprets the current number under the new mode).
- A **number field**: in % mode shows `discountPercent` with a `%` suffix; in Rs mode shows `discount` with the `Rs` prefix. On change → `vm.applyDiscount(discountIsPercent, typedValue)`.
- A row of **preset chips: `5%` · `10%` · `15%` · `Clear`** — each is one tap: `5/10/15` → `vm.applyDiscount(isPercent = true, n)`; `Clear` → `vm.applyDiscount(isPercent = false, 0)`.
- The cashier can still type a custom % or Rs.

## Related fix (POS live ticket)
`PosSaleScreen` shows the cart's Discount row as a **hard-coded `"— Rs 0"`** (`TotalRow("Discount", "— Rs 0", true)`). Wire it to the real value: `TotalRow("Discount", if (vm.discount > 0) "— " + rs(vm.discount) else "Rs 0", true)`. Small correctness fix, directly related to this feature.

## Edge handling
- Percent clamped 0–100 at input; `discount` is still clamped to `subtotal` by `clampedDiscount`, so no free/negative sale.
- `total`, `change`, and the "Received/Change" figures update live (the re-sync mirrors the existing field's behaviour).
- Switching modes re-interprets the current number; an empty field reads as 0.

## Files
**Create:** `ui/sale/DiscountCalc.kt` (`percentToFlat`), `test/.../DiscountCalcTest.kt`; the `DiscountField` composable (private, in `PosCheckoutScreen.kt`).
**Modify:** `ui/sale/SellingViewModel.kt` (state + `applyDiscount` + `startNewTicket`), `ui/checkout/PosCheckoutScreen.kt` (DiscountField replaces the discount NumField), `ui/sale/PosSaleScreen.kt` (Discount row).

## Testing
- **Unit** (`DiscountCalcTest`): `percentToFlat` — 10% of 1000 = 100; rounding (e.g. 7% of 1455); pct clamp (>100 → 100, <0 → 0).
- **On-device**: tap `10%` → correct Rs off the total; toggle to Rs → type a flat amount; `Clear` → 0; total/change update live; POS ticket Discount row shows the real value.

## Out of scope
Per-line discounts; persisting the % onto the sale record; discounting shipping.
