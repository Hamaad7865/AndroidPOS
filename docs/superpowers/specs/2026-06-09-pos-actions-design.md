# POS Action Bar (Pillar B1) — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/pos-actions` (stacked on `feature/pos-reorder`, off `main`). **No DB change.**

> Pillar B of the customizable-POS request. **B1** (this spec) = the Actions bar + Item lookup, Price override, Reprint, Void, Exit. **B2** (Remarks, needs a `Sale.note` migration) is a separate later spec.

## Purpose
Add a POS **Actions bar** that surfaces the existing actions for discoverability and adds new ones, matching the reference POS's button panel.

## The bar (`PosSaleScreen` ticket panel)
- A compact grid of small labelled buttons in the right-hand ticket panel, just **above the Charge button**: `Customer · Hold · Returns · Item lookup · Price · Reprint · Void · Exit`. (The `Remarks` button arrives in B2.)
- The existing big **Charge** button stays; Scan / New / Arrange stay in the search row.
- `Customer`, `Hold`, `Returns` trigger the existing flows (open the customer picker, hold the ticket, go to Returns). The rest are below.

## New functions
1. **Item lookup** — a dialog with a search field + a scrollable list of products showing **name · price · stock** (read-only; does not add to the cart). Reuses `vm.products`. For "how much is this / do you have it in stock".
2. **Price override** (per line) — `PosLine` gains `priceOverride: Int? = null`; `effectivePrice = priceOverride ?: product.price`; `lineTotal = effectivePrice * qty`. `SellingViewModel.setLinePrice(productId, price)`. The **Price** button opens a dialog listing the cart lines; tapping a line reveals a price field that sets its unit price *for this sale only* (catalog price untouched). `persist()` writes `unitPriceCents`/`lineTotalCents` from `effectivePrice`. **No schema change** (`SaleItem.unitPriceCents` already exists).
3. **Reprint last receipt** — if `vm.lastSale != null`, navigate to the existing receipt screen showing it; otherwise a toast "No recent sale to reprint."
4. **Void** — clears the current ticket (`vm.startNewTicket()`) behind a confirm dialog ("Void this ticket?").
5. **Exit** — navigate to Home.

## Testing
- **Unit** (`SellingViewModelTest`): a line with a `priceOverride` uses it for `lineTotal`/`subtotal`/`total`; a completed sale persists `unitPriceCents`/`lineTotalCents` from the override.
- **On-device**: Item lookup shows price + stock without adding; Price override changes a line's price + the totals; Reprint opens the last receipt; Void clears (after confirm); Exit → Home.

## Files
**Modify:** `ui/sale/SellingViewModel.kt` (PosLine.priceOverride + effectivePrice + setLinePrice + persist), `ui/sale/PosSaleScreen.kt` (Actions bar + Item-lookup dialog + Price-override dialog + Void confirm + Reprint/Exit navigation).

## Out of scope
Remarks (B2); SHIP FROM (multi-location); adding to the cart from Item lookup; persisting the price override reason.
