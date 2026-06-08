# VAT Types & Registration — Design Spec

**Date:** 2026-06-08 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/vat-types` (off `main`).

## Purpose
Today VAT is a hard-coded global **15% inclusive** at the till. This adds:
1. A **non-VAT-registered** mode (some clients don't charge VAT at all).
2. **Per-product VAT classification** (MRA standard): **Standard 15% / Exempt 0% / Zero-rated 0%**, chosen from a dropdown on the product.

## Model — two layers
1. **Global:** `BusinessProfile.vatRegistered: Boolean` (default **true**). When **off** → no VAT anywhere, and the VAT registration number is hidden on receipts.
2. **Per-product:** `Product.vatType ∈ {STANDARD, EXEMPT, ZERO_RATED}` (default **STANDARD**). Standard = 15% inclusive; **Exempt and Zero-rated are both 0%** at the till — the distinction is stored for future reporting (zero-rated can reclaim input VAT, exempt can't).

## Key facts (why this is low-risk)
- Prices are **VAT-inclusive**; VAT is informational and is **never added to the total**. So every VAT change here leaves prices and totals **unchanged** — it only affects what VAT figure is computed, shown, and printed.
- The `Product` entity already has `taxRatePercent`/`taxInclusive` columns and a "Tax" card on the form; the till's VAT calc just ignored them (used a global 15%). We replace that with the `vatType` model.

## Data model & migration
- **`VatType` enum** (data layer): `STANDARD(15.0, "Standard (15%)")`, `EXEMPT(0.0, "Exempt")`, `ZERO_RATED(0.0, "Zero-rated")`, with `from(id)` / `ratePercent` / `label`.
- **`Product.vatType: String`** new column, default `"STANDARD"`.
- **Room migration v6 → v7** (non-destructive): `ALTER TABLE products ADD COLUMN vatType TEXT NOT NULL DEFAULT 'STANDARD'`. Registered via `.addMigrations(MIGRATION_6_7)` on the Room builder; `fallbackToDestructiveMigration` stays as a last resort only. **Existing products & sales are preserved** (existing rows default to STANDARD → unchanged 15% behaviour). DB `version = 7`.
- **`PosProduct.vatType: VatType`** new field; `CatalogMapping.toPosProducts` parses it from `Product.vatType`.
- On product save, `taxRatePercent` is kept consistent (Standard→15, else→0) for any legacy reader, but **`vatType` is the source of truth** for the till.

## Till calculation (`SellingViewModel`)
- Extract a **pure, unit-tested helper**: `fun vatOf(lines: List<PosLine>, vatRegistered: Boolean): Int` =
  `if (!vatRegistered) 0 else lines.sumOf { if (it.product.vatType == STANDARD) it.lineTotal - (it.lineTotal / 1.15).roundToInt() else 0 }`.
- `vm.vat` delegates to it. `total` is unchanged (`subtotal − discount + shipping`).
- The VM gains `var vatRegistered: Boolean = true`; the POS/checkout composables sync it from `BusinessProfile.vatRegistered(context)` in a `LaunchedEffect` (VMs are context-free in this codebase). The stored `SaleSnapshot.vat` therefore reflects the business + per-line types at sale time (historical sales keep their recorded value).

## Product Add/Edit UI (`ProductsScreen.kt`, "Tax" card)
- Replace the "VAT rate (0/15)" `PickerField` with a **"VAT type"** dropdown → *Standard (15%) / Exempt / Zero-rated* (default Standard, so most products need no extra tap).
- **Remove** the "Inclusive/Exclusive" `PickerField` (the app is always VAT-inclusive; exclusive was never wired into the till). Keep `taxInclusive = true` on save.
- `AddProductScreen` state: `var vatType by remember { mutableStateOf(VatType.STANDARD) }`, loaded from the product on edit; passed to `vm.saveProduct(... vatType ...)`.

## Receipts & checkout display
- **`PosCheckoutScreen`** breakdown: show the VAT row only when `vatRegistered`; value = `vm.vat`.
- **`PosReceiptScreen`** (on-screen) + **`ReceiptOutput`** (PDF Canvas + print HTML): hide the VAT line when not registered.
- **`BusinessProfile.receiptLines`**: when not registered, omit the `VAT <number>` part (keep BRN). These sites have `context`, so they read `BusinessProfile.vatRegistered(context)` directly.

## Settings
- Add a **"VAT-registered" switch** to the **Business profile dialog** in `SettingsScreen` (alongside name/address/BRN/VAT number — registration + VAT number belong together). `BusinessProfile.setProfile(...)` gains the flag. When off, the VAT-number field is visually de-emphasised/optional.

## Reports
- **No change.** Reports sum each sale's stored `vat`, which is already correct (0 for non-VAT / all-exempt sales). The per-product type is persisted, so a type breakdown can be added later.

## Testing
- **Unit** (`VatCalcTest` or similar): the pure `vatOf(...)` — all-standard, mixed Standard/Exempt/Zero, all-exempt, and `vatRegistered = false`.
- **Migration**: a Room migration test (v6→v7) confirming the column is added with default `STANDARD` and existing rows survive.
- **Manual on emulator**: set a product to Zero-rated/Exempt → its line adds 0 VAT at checkout; toggle the business to *not VAT-registered* → VAT line and VAT number disappear from checkout, receipt, and PDF; build + detekt + ktlint clean.

## Files
**Create:** `data/entity/VatType.kt` (enum); migration (in `PosDatabase.kt` or a `migrations` file); `test/.../VatCalcTest.kt`.
**Modify:** `Product.kt` (vatType), `PosDatabase.kt` (version 7 + migration), `di/AppContainer.kt` (addMigrations), `CatalogMapping.kt`, `SellingViewModel.kt` (PosProduct.vatType + vat calc + vatRegistered), `CatalogViewModel.saveProduct`, `ProductsScreen.kt` (VAT-type dropdown), `BusinessProfile.kt` (vatRegistered + receiptLines), `SettingsScreen.kt` (profile dialog toggle), `PosCheckoutScreen.kt`, `PosReceiptScreen.kt`, `ReceiptOutput.kt`.

## Out of scope
Per-type receipt/report breakdown; exclusive (added-on-top) VAT; editable VAT rate (fixed 15% for Standard).
