# Sub-categories (main → subs) — Design Spec

**Date:** 2026-06-09 · **Status:** approved, ready for implementation plan.
**Branch:** `feature/sub-categories` (stacked on `feature/pos-discount` / PR #8 → `feature/vat-types` / PR #7). **DB v8 → v9.** Merge order: #7 → #8 → this.

## Purpose
Nest categories one level: a **main category** can have many **sub-categories**. A product picks a main and an *optional* sub. The POS and Products category filters **drill down** (main chips → sub chips).

## Data model
- **`Category`** gains `parentId: Long?` — `null` = a main category; a value = a sub under that main's id.
- **`Product.categoryId` is unchanged.** It points to whichever category the user picked — a main *or* a sub. A product's main is derived: `leaf.parentId ?: leaf.id`.
- **Migration v8→v9:** `ALTER TABLE categories ADD COLUMN parentId INTEGER` (nullable — no `NOT NULL`, no default; existing rows become mains with `parentId = null`). Non-destructive; Room's expected schema for a nullable `parentId` with no default matches the migration. Commit the exported `9.json`; migration-tested on-device.

## Category tree (pure, testable) — new `ui/sale/CategoryTree.kt`
- `data class MainCat(val id: Long, val name: String, val subs: List<Category>)`.
- `fun buildCategoryTree(all: List<Category>): List<MainCat>` — mains = `parentId == null` (sorted by sortOrder, name); each main's `subs` = categories whose `parentId == main.id`.
- `fun mainIdOf(leaf: Category): Long = leaf.parentId ?: leaf.id`.
- Filter is expressed on the display model (see PosProduct below), not raw entities.

## PosProduct mapping (`CatalogMapping.kt`)
- `PosProduct` gains `mainCat: String` (the main category's name). `cat` stays the **leaf** name (the sub's name if the product is in a sub, else the main's name).
- `toPosProducts(categories)`: for each product, `leaf = catById[categoryId]`; `cat = leaf?.name ?: "Other"`; `mainCat = name of (leaf.parentId ?: leaf.id)`, falling back to `cat`.
- Replace `toFilterLabels()` with `buildCategoryTree(...)` consumed by the chip rows.
- **Display label** for a product = `if (cat != mainCat && mainCat.isNotEmpty()) "$mainCat · $cat" else cat`.

## Filtering UI — drill-down (`PosSaleScreen` + `ProductsScreen`)
- State: `selectedMain: String?` (null = All) and `selectedSub: String?` (null = all of the main).
- **Row 1 chips:** `All` + main names. Tapping a main sets `selectedMain` and clears `selectedSub`.
- **Row 2 chips** (only when a main is selected): `All <main>` + that main's sub names. Tapping a sub sets `selectedSub`.
- **Predicate** (on `PosProduct`): matches if `selectedMain == null` OR (`p.effectiveMain == selectedMain` AND (`selectedSub == null` OR `p.cat == selectedSub`)), where `effectiveMain = mainCat.ifEmpty { cat }`.

## Product form (`ProductsScreen` + `CatalogViewModel`)
- Replace the single "Category" `PickerField` with **"Main category"** + **"Sub-category (optional)"**.
- Main options = main names; Sub options = the chosen main's subs (typing a new value auto-creates it). Changing the main resets the sub field.
- `saveProduct(mainCategoryName, subCategoryName, …)`: resolve/create the main (`Category(name, parentId = null)`); if `subCategoryName` is non-blank, resolve/create the sub (find by name with `parentId == mainId`, else `Category(name, parentId = mainId)`); set `categoryId = subId ?: mainId`.
- Edit-load: derive the main + sub names from the product's leaf category to prefill both fields.

## Repository
- `upsertCategory(Category)` already exists and now carries `parentId`. Sub resolution (find an existing sub by name under a main) is done in the ViewModel against the observed category list (same pattern as the current main lookup). No new DAO query required.

## Migration safety & test
- On-device v8→v9: install over the current v8 DB and confirm existing categories + products survive (categories become mains). Inspect `9.json` for `parentId` (nullable).

## Testing
- **Unit** (`CategoryTreeTest`): `buildCategoryTree` groups mains + subs; `mainIdOf`; the drill-down predicate (All / main / main+sub); the display label.
- **On-device:** create a main and a sub, assign a product to the sub, drill-down filter on POS + Products, and confirm the migration preserves data.

## Files
**Create:** `ui/sale/CategoryTree.kt` + `test/.../CategoryTreeTest.kt`; `MIGRATION_8_9` (in `PosDatabase.kt`).
**Modify:** `data/entity/Category.kt`, `data/PosDatabase.kt` (v9 + migration), `di/AppContainer.kt`, `ui/sale/CatalogMapping.kt` (PosProduct.mainCat + tree), `ui/sale/PosProduct` (field), `ui/sale/SellingViewModel.kt` + `ui/sale/PosSaleScreen.kt` (drill-down chips + filter), `ui/products/CatalogViewModel.kt` (main+sub save + tree) + `ui/products/ProductsScreen.kt` (pickers, drill-down chips, list label).

## Out of scope
More than two levels; a dedicated category-management screen (subs auto-create on save, like main categories today); renaming / reordering / deleting categories.
