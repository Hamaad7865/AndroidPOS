# POS Reorderable Product Grid (Pillar A) — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/pos-reorder` (off `main`). **No DB change / no migration** — the order is a saved setting.

> Part of a larger "customizable POS" request. **Pillar A** (this spec) = reorder the catalog tiles. **Pillar B** (POS action-button bar) is a separate later effort.

## Purpose
Let the shopkeeper arrange the POS product tiles into a custom order that persists. Tap-to-add, barcode scan, category drill-down, and search are unchanged.

## Persistence (no migration)
- **`PosLayoutPrefs`** (mirrors `BusinessProfile` — SharedPreferences `EncryptedSharedPreferences`/plain prefs as `BusinessProfile` uses):
  - `order(context): List<String>` — the saved product-id order (empty = none).
  - `setOrder(context, ids: List<String>)` — persist (store as a newline-joined string).
  - `clear(context)` — wipe the custom order.

## Ordering helper (pure, testable) — new `ui/sale/ProductOrder.kt`
```kotlin
fun orderProducts(products: List<PosProduct>, savedOrder: List<String>): List<PosProduct>
```
- Sort by the product's index in `savedOrder` (id not present → `Int.MAX_VALUE`), tiebreak by `name` case-insensitively. So saved items lead in their saved order; everything else follows A–Z. Empty `savedOrder` → pure A–Z.

## POS screen (`PosSaleScreen`)
- On entry, read `PosLayoutPrefs.order(context)` into a `remember` state `order`.
- Apply it **before** filtering: `val ordered = orderProducts(vm.products, order)`; then `visible = ordered.filter { matchesCategory(...) && query }`. (Today's grid is already a custom 4-column grid over `visible` — unchanged rendering.)
- **"Arrange" toggle** in the product-area header (next to search / category chips). `var arrangeMode by remember { mutableStateOf(false) }`, `var pickedId by remember { mutableStateOf<String?>(null) }`.
- **Arrange mode behaviour:**
  - Tapping a tile does **not** add to cart.
  - No tile picked → tap a tile to **pick it up** (highlight it; show a banner "Moving <name> — tap where it should go").
  - A tile picked → tap another tile to **drop `pickedId` immediately before that tile** in the global order; persist; clear `pickedId`.
  - Tap the picked tile again → cancel the pick.
  - Header shows **Done** (exit arrange) and **Reset A–Z** (`PosLayoutPrefs.clear`, `order = emptyList()`).
- **Move logic:** `fullIds = orderProducts(vm.products, order).map { it.id }`; `newIds = (fullIds - pickedId)` with `pickedId` inserted at the index of the target id; `PosLayoutPrefs.setOrder(context, newIds)`; `order = newIds`.

## Behaviour notes
- Normal (non-arrange) mode is unchanged — tap adds, scan adds, categories + search filter.
- The saved order is **global**; filters narrow what's shown but preserve relative order. Arranging within a filtered view repositions within the global order.
- New products (not in the saved order) appear at the end A–Z until placed.

## Testing
- **Unit** (`ProductOrderTest`): `orderProducts` — empty order = A–Z; saved order respected; ids not in the catalog ignored; products missing from the order fall to the end A–Z.
- **On-device:** enter Arrange, move a tile, Done; confirm the order shows on the POS and survives an app restart; Reset A–Z restores alphabetical.

## Files
**Create:** `data/profile/PosLayoutPrefs.kt`, `ui/sale/ProductOrder.kt` + `test/.../ProductOrderTest.kt`.
**Modify:** `ui/sale/PosSaleScreen.kt` (apply order, Arrange toggle + pick/place, Done/Reset).

## Out of scope
Per-category orders; animated drag-and-drop (pick-and-place gives the same control reliably); the Pillar B action-button bar.
