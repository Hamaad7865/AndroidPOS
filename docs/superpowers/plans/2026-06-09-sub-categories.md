# Sub-categories (main → subs) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nest categories one level — a main category holds many sub-categories; products pick a main + optional sub; the POS and Products filters drill down (main chips → sub chips).

**Architecture:** `Category` gains a self-referential `parentId` (non-destructive v8→v9 migration). `Product.categoryId` is unchanged — it points at the chosen leaf (main or sub); the main is derived. A pure `CategoryTree.kt` builds the main/sub tree and the drill-down predicate; both VMs expose the tree and both screens render two chip rows.

**Tech Stack:** Kotlin, Jetpack Compose, Room (SQLCipher), JUnit4. Spec: `docs/superpowers/specs/2026-06-09-sub-categories-design.md`. Branch `feature/sub-categories` (stacked on `feature/pos-discount`, DB v9). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation; add `--rerun-tasks --no-build-cache` to tests when stale.

---

## File structure
**Create:** `ui/sale/CategoryTree.kt` + `test/.../CategoryTreeTest.kt`; `MIGRATION_8_9` (in `PosDatabase.kt`).
**Modify:** `data/entity/Category.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`, `ui/sale/SellingViewModel.kt` (PosProduct.mainCat + categoryTree), `ui/sale/CatalogMapping.kt`, `ui/sale/PosSaleScreen.kt`, `ui/products/CatalogViewModel.kt`, `ui/products/ProductsScreen.kt`.

---

## Task 1: Category.parentId + migration v8→v9

**Files:** Modify `data/entity/Category.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`

- [ ] **Step 1: Add the column** — in `Category.kt`, change the data class to:
```kotlin
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** Parent main category id; null = this IS a main category. A nullable column needs no
     *  SQL default, so Room's expected schema matches the v8→v9 migration (no destructive wipe). */
    val parentId: Long? = null,
)
```

- [ ] **Step 2: Migration + version bump** — in `PosDatabase.kt`, change `version = 8,` to `version = 9,`, add the comment `// v9: categories gained a parentId column for sub-categories (additive, non-destructive MIGRATION_8_9).` above it, and add below `MIGRATION_7_8`:
```kotlin

/** v8→v9: add categories.parentId (nullable) for sub-categories; existing rows stay mains (null). */
val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")
        }
    }
```

- [ ] **Step 3: Register it** — in `AppContainer.kt`, add `import com.nexapos.retail.data.MIGRATION_8_9`, and change `.addMigrations(MIGRATION_6_7, MIGRATION_7_8)` to `.addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)`.

- [ ] **Step 4: Build + check schema** — `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug` → BUILD SUCCESSFUL. Confirm the export: `find app -name "9.json" -path "*PosDatabase*" | head -1 | xargs grep -o '"parentId"'` prints a match (the column is present and nullable — no `notNull` true / no default).

- [ ] **Step 5: Commit** (include `app/schemas/.../9.json`)
```bash
git add app/src/main/java/com/nexapos/retail/data/entity/Category.kt app/src/main/java/com/nexapos/retail/data/PosDatabase.kt app/src/main/java/com/nexapos/retail/di/AppContainer.kt app/schemas/com.nexapos.retail.data.PosDatabase/9.json
git commit -m "feat: categories.parentId column + non-destructive migration v8->v9"
```

---

## Task 2: CategoryTree pure helpers (TDD)

**Files:** Create `ui/sale/CategoryTree.kt`, `test/.../CategoryTreeTest.kt`

- [ ] **Step 1: Write the failing tests**
```kotlin
package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class CategoryTreeTest {
    private val plumbing = Category(id = 1, name = "Plumbing", parentId = null)
    private val pipes = Category(id = 2, name = "Pipes", parentId = 1)
    private val fittings = Category(id = 3, name = "Fittings", parentId = 1)
    private val electrical = Category(id = 4, name = "Electrical", parentId = null)

    @Test
    fun `tree groups mains with their subs`() {
        val tree = buildCategoryTree(listOf(plumbing, pipes, fittings, electrical))
        assertEquals(listOf("Electrical", "Plumbing"), tree.map { it.name }) // sorted
        val plumb = tree.first { it.name == "Plumbing" }
        assertEquals(listOf("Fittings", "Pipes"), plumb.subs.map { it.name })
    }

    @Test
    fun `mainIdOf returns parent for a sub and self for a main`() {
        assertEquals(1L, mainIdOf(pipes))
        assertEquals(1L, mainIdOf(plumbing))
    }

    @Test
    fun `predicate matches all when no main selected`() {
        assertTrue(matchesCategory("Plumbing", "Pipes", main = null, sub = null))
    }

    @Test
    fun `predicate narrows by main then sub`() {
        assertTrue(matchesCategory("Plumbing", "Pipes", main = "Plumbing", sub = null))
        assertFalse(matchesCategory("Electrical", "Wire", main = "Plumbing", sub = null))
        assertTrue(matchesCategory("Plumbing", "Pipes", main = "Plumbing", sub = "Pipes"))
        assertFalse(matchesCategory("Plumbing", "Fittings", main = "Plumbing", sub = "Pipes"))
    }

    @Test
    fun `label shows main and sub only when distinct`() {
        assertEquals("Plumbing · Pipes", categoryLabel("Plumbing", "Pipes"))
        assertEquals("Plumbing", categoryLabel("Plumbing", "Plumbing"))
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*CategoryTreeTest*" --rerun-tasks --no-build-cache` → FAIL (unresolved).

- [ ] **Step 3: Implement** — create `CategoryTree.kt`:
```kotlin
package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Category

/** A main category together with its sub-categories. */
data class MainCat(
    val id: Long,
    val name: String,
    val subs: List<Category>,
)

/** Groups flat categories into mains (parentId == null), each carrying its subs, both sorted. */
fun buildCategoryTree(all: List<Category>): List<MainCat> {
    val subsByParent = all.filter { it.parentId != null }.groupBy { it.parentId }
    return all.filter { it.parentId == null }
        .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        .map { main ->
            MainCat(
                id = main.id,
                name = main.name,
                subs = (subsByParent[main.id] ?: emptyList()).sortedWith(compareBy({ it.sortOrder }, { it.name })),
            )
        }
}

/** The main-category id for a leaf category (itself when it is already a main). */
fun mainIdOf(leaf: Category): Long = leaf.parentId ?: leaf.id

/**
 * Drill-down filter. [main]/[sub] are the selected names (null = no filter at that level);
 * [productMain]/[productLeaf] describe the product's category.
 */
fun matchesCategory(
    productMain: String,
    productLeaf: String,
    main: String?,
    sub: String?,
): Boolean = main == null || (productMain == main && (sub == null || productLeaf == sub))

/** Product category label: "Main · Sub" when it has a distinct sub, else just the name. */
fun categoryLabel(
    mainCat: String,
    leaf: String,
): String = if (mainCat.isNotEmpty() && leaf != mainCat) "$mainCat · $leaf" else leaf
```

- [ ] **Step 4: Run to verify it passes** — same command → PASS.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/CategoryTree.kt app/src/test/java/com/nexapos/retail/ui/sale/CategoryTreeTest.kt
git commit -m "feat: CategoryTree helpers (tree, mainIdOf, drill-down predicate, label) + tests"
```

---

## Task 3: PosProduct.mainCat + mapping

**Files:** Modify `ui/sale/SellingViewModel.kt`, `ui/sale/CatalogMapping.kt`

- [ ] **Step 1: Add the field** — in `SellingViewModel.kt` `PosProduct`, add after `val cat: String,`:
```kotlin
    /** The product's MAIN category name (= cat when the product sits directly under a main). */
    val mainCat: String = "",
```

- [ ] **Step 2: Derive it in the mapping** — in `CatalogMapping.kt` `toPosProducts`, replace:
```kotlin
    val catById = categories.associate { it.id to it.name }
    val brandById = brands.associate { it.id to it.name }
    return map { product ->
        PosProduct(
            id = product.id.toString(),
            name = product.name,
            cat = catById[product.categoryId] ?: UNCATEGORISED,
```
with:
```kotlin
    val catById = categories.associateBy { it.id }
    val nameById = categories.associate { it.id to it.name }
    val brandById = brands.associate { it.id to it.name }
    return map { product ->
        val leaf = catById[product.categoryId]
        val leafName = leaf?.name ?: UNCATEGORISED
        val mainName = leaf?.let { nameById[mainIdOf(it)] } ?: leafName
        PosProduct(
            id = product.id.toString(),
            name = product.name,
            cat = leafName,
            mainCat = mainName,
```
(Leave the rest of the `PosProduct(...)` arguments unchanged — `cat` keeps its position; `mainCat` is added right after it.)

- [ ] **Step 3: Replace `toFilterLabels` with the tree** — in `CatalogMapping.kt`, replace:
```kotlin
/** Category chip labels, always starting with [ALL_CATEGORY]. */
fun List<Category>.toFilterLabels(): List<String> = listOf(ALL_CATEGORY) + map { it.name }
```
with:
```kotlin
/** The category tree (mains + their subs) used by the drill-down chip rows. */
fun List<Category>.toCategoryTree(): List<MainCat> = buildCategoryTree(this)
```

- [ ] **Step 4: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` will FAIL where `toFilterLabels`/`categories` are still used (Tasks 4–5 fix those). That is expected; proceed to Task 4 and build at the end of Task 5.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt app/src/main/java/com/nexapos/retail/ui/sale/CatalogMapping.kt
git commit -m "feat: PosProduct.mainCat + category tree mapping"
```

---

## Task 4: POS drill-down (SellingViewModel + PosSaleScreen)

**Files:** Modify `ui/sale/SellingViewModel.kt`, `ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Expose the tree** — in `SellingViewModel.kt`, replace:
```kotlin
    /** Category filter labels, always starting with "All". */
    var categories by mutableStateOf(listOf(ALL_CATEGORY))
        private set
```
with:
```kotlin
    /** Category tree (mains + subs) for the drill-down filter chips. */
    var categoryTree by mutableStateOf<List<MainCat>>(emptyList())
        private set
```
and in the `init` collect block replace `categories = labels` — first change the producer. Find:
```kotlin
                prods.toPosProducts(cats) to cats.toFilterLabels()
```
replace with:
```kotlin
                prods.toPosProducts(cats) to cats.toCategoryTree()
```
and `categories = labels` →
```kotlin
                categoryTree = labels
```
(`labels` is now the tree; the local name is fine.)

- [ ] **Step 2: Drill-down chips + filter in `PosSaleScreen`** — replace the single `cat` state. Find `var cat by remember { mutableStateOf("All") }` (near the other `remember`s at the top of the composable) and replace with:
```kotlin
    var selMain by remember { mutableStateOf<String?>(null) }
    var selSub by remember { mutableStateOf<String?>(null) }
```
Replace the visible filter (lines ~86-90):
```kotlin
    val visible =
        vm.products.filter {
            (cat == "All" || it.cat == cat) &&
                (query.isBlank() || (it.name + " " + it.sku).contains(query, ignoreCase = true))
        }
```
with:
```kotlin
    val visible =
        vm.products.filter {
            matchesCategory(it.mainCat.ifEmpty { it.cat }, it.cat, selMain, selSub) &&
                (query.isBlank() || (it.name + " " + it.sku).contains(query, ignoreCase = true))
        }
```
Replace the chip Row (lines ~193-204) with two rows:
```kotlin
                    // Category chips — main row, then a sub row when a main is selected.
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryChip("All", selMain == null, vm.products.size) { selMain = null; selSub = null }
                        vm.categoryTree.forEach { m ->
                            val count = vm.products.count { (it.mainCat.ifEmpty { it.cat }) == m.name }
                            CategoryChip(m.name, selMain == m.name, count) { selMain = m.name; selSub = null }
                        }
                    }
                    val mainSel = vm.categoryTree.firstOrNull { it.name == selMain }
                    if (mainSel != null && mainSel.subs.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryChip("All ${mainSel.name}", selSub == null, vm.products.count { (it.mainCat.ifEmpty { it.cat }) == mainSel.name }) { selSub = null }
                            mainSel.subs.forEach { s ->
                                CategoryChip(s.name, selSub == s.name, vm.products.count { it.cat == s.name }) { selSub = s.name }
                            }
                        }
                    }
```

- [ ] **Step 3: Build** — proceed to Task 5; build at the end of Task 5.
- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "feat: POS drill-down category chips (main -> sub)"
```

---

## Task 5: Products screen — tree, form Main+Sub pickers, drill-down, label

**Files:** Modify `ui/products/CatalogViewModel.kt`, `ui/products/ProductsScreen.kt`

- [ ] **Step 1: Expose the tree + main/sub save in `CatalogViewModel`** — replace:
```kotlin
    var categories by mutableStateOf(listOf(ALL_CATEGORY))
        private set
```
with:
```kotlin
    var categoryTree by mutableStateOf<List<MainCat>>(emptyList())
        private set
```
In the collect block replace `categories = cats.toFilterLabels()` with `categoryTree = cats.toCategoryTree()`.
Change `saveProduct`'s `categoryName: String,` parameter to:
```kotlin
        mainCategoryName: String,
        subCategoryName: String,
```
and replace the category-resolution block:
```kotlin
        val trimmedCat = categoryName.trim()
        val trimmedBrand = brandName.trim()
        viewModelScope.launch {
            val categoryId =
                resolveLookupId(
                    name = trimmedCat,
                    existing = categoryEntities.firstOrNull { it.name.equals(trimmedCat, ignoreCase = true) }?.id,
                ) { catalogRepository.upsertCategory(Category(name = trimmedCat)) }
```
with:
```kotlin
        val trimmedMain = mainCategoryName.trim()
        val trimmedSub = subCategoryName.trim()
        val trimmedBrand = brandName.trim()
        viewModelScope.launch {
            val mainId =
                if (trimmedMain.isBlank()) {
                    null
                } else {
                    categoryEntities.firstOrNull { it.parentId == null && it.name.equals(trimmedMain, ignoreCase = true) }?.id
                        ?: catalogRepository.upsertCategory(Category(name = trimmedMain, parentId = null))
                }
            val categoryId =
                when {
                    mainId == null -> null
                    trimmedSub.isBlank() -> mainId
                    else ->
                        categoryEntities.firstOrNull { it.parentId == mainId && it.name.equals(trimmedSub, ignoreCase = true) }?.id
                            ?: catalogRepository.upsertCategory(Category(name = trimmedSub, parentId = mainId))
                }
```
(The rest of `saveProduct` — `brandId`, `base.copy(... categoryId = categoryId ...)` — is unchanged.)

- [ ] **Step 2: Build helper for edit-load** — add a small public function to `CatalogViewModel` so the form can prefill main + sub from a product:
```kotlin
    /** (mainName, subName) for a product id; subName is "" when the product sits under a main. */
    fun categoryNamesFor(productId: Long): Pair<String, String> {
        val leaf = categoryEntities.firstOrNull { it.id == catalogIdOf(productId) } ?: return "" to ""
        return if (leaf.parentId == null) {
            leaf.name to ""
        } else {
            (categoryEntities.firstOrNull { it.id == leaf.parentId }?.name ?: "") to leaf.name
        }
    }

    private fun catalogIdOf(productId: Long): Long? =
        products.firstOrNull { it.id == productId.toString() }?.let { pp ->
            categoryEntities.firstOrNull { it.name == pp.cat && (it.parentId != null) == (pp.cat != pp.mainCat) }?.id
        }
```
> Simpler + robust: resolve directly from the entity. Replace the two functions above with one that reads the product entity:
```kotlin
    /** (mainName, subName) for a product; subName is "" when it sits directly under a main. */
    suspend fun categoryNamesFor(productId: Long): Pair<String, String> {
        val catId = catalogRepository.getProduct(productId)?.categoryId ?: return "" to ""
        val leaf = categoryEntities.firstOrNull { it.id == catId } ?: return "" to ""
        return if (leaf.parentId == null) {
            leaf.name to ""
        } else {
            (categoryEntities.firstOrNull { it.id == leaf.parentId }?.name ?: "") to leaf.name
        }
    }
```
Use only this `suspend` version; delete the first two-function sketch.

- [ ] **Step 3: Form pickers in `ProductsScreen`** — replace the `var category` state. Find `var category by remember { mutableStateOf("") }` and replace with:
```kotlin
    var mainCategory by remember { mutableStateOf("") }
    var subCategory by remember { mutableStateOf("") }
```
In the edit-load `LaunchedEffect`, replace:
```kotlin
                category = posModel?.cat?.takeIf { it != "Other" } ?: ""
```
with:
```kotlin
                val (mn, sn) = vm.categoryNamesFor(p.id)
                mainCategory = mn
                subCategory = sn
```
Replace the Classification "Category" `PickerField` (lines ~785-792) with two pickers:
```kotlin
                        PickerField(
                            "Main category",
                            mainCategory,
                            options = vm.categoryTree.map { it.name },
                            onValueChange = { mainCategory = it; subCategory = "" },
                            Modifier.weight(1f),
                            placeholder = "Tools, Plumbing, Paint…",
                        )
                        PickerField(
                            "Sub-category (optional)",
                            subCategory,
                            options = vm.categoryTree.firstOrNull { it.name.equals(mainCategory, ignoreCase = true) }?.subs?.map { it.name } ?: emptyList(),
                            onValueChange = { subCategory = it },
                            Modifier.weight(1f),
                            placeholder = "Pipes, Fittings…",
                        )
```
Update the `vm.saveProduct(...)` call — replace `categoryName = category,` with:
```kotlin
            mainCategoryName = mainCategory,
            subCategoryName = subCategory,
```

- [ ] **Step 4: Drill-down chips + filter + label in `ProductsScreen`** — replace `var chip by remember { mutableStateOf("All") }` with:
```kotlin
    var selMain by remember { mutableStateOf<String?>(null) }
    var selSub by remember { mutableStateOf<String?>(null) }
```
In the `rows` filter (lines ~118-121), change the `remember(...)` keys `chip` → `selMain, selSub` and replace:
```kotlin
                    val catOk = chip == "All" || p.cat == chip
```
with:
```kotlin
                    val catOk = matchesCategory(p.mainCat.ifEmpty { p.cat }, p.cat, selMain, selSub)
```
Replace the chip row (line ~247) `vm.categories.forEach { Chip(it, chip == it) { chip = it } }` with:
```kotlin
                Chip("All", selMain == null) { selMain = null; selSub = null }
                vm.categoryTree.forEach { m -> Chip(m.name, selMain == m.name) { selMain = m.name; selSub = null } }
```
Immediately after that chip Row, add the sub row:
```kotlin
            val mainSel = vm.categoryTree.firstOrNull { it.name == selMain }
            if (mainSel != null && mainSel.subs.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip("All ${mainSel.name}", selSub == null) { selSub = null }
                    mainSel.subs.forEach { s -> Chip(s.name, selSub == s.name) { selSub = s.name } }
                }
            }
```
(If the existing chip Row isn't wrapped so a sibling Row can follow, wrap both in a `Column`. Confirm by reading lines 240-250 first.)
Update the subtitle (line ~212) `${(vm.categories.size - 1).coerceAtLeast(0)} categories` → `${vm.categoryTree.size} categories`.
Update the list category cell (line ~344) `Text(p.cat, …)` → `Text(categoryLabel(p.mainCat, p.cat), …)`.
Update the `filterActive`/empty-state text referencing the chip if needed (search for `chip` and replace remaining references with `selMain`).

- [ ] **Step 5: Format, build, full check** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --continue --no-build-cache` → BUILD SUCCESSFUL. Fix any leftover `categories`/`chip`/`toFilterLabels` references the compiler flags.
- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/products/CatalogViewModel.kt app/src/main/java/com/nexapos/retail/ui/products/ProductsScreen.kt
git commit -m "feat: Products screen sub-categories — main+sub pickers, drill-down filter, labels"
```

---

## Task 6: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL; `CategoryTreeTest` green.
- [ ] **Step 2: Migration test (v8→v9 preserves data)** — `adb install -r app/build/outputs/apk/debug/app-debug.apk` over the existing v8 emulator DB; relaunch, sign in → the existing product/categories are still present (existing categories are now mains).
- [ ] **Step 3: On-device feature test** — edit a product → set Main = "Plumbing", Sub = "Pipes" (type new) → save. Add another under "Plumbing" with no sub. On POS + Products: the main row shows "Plumbing"; tapping it reveals the sub row ("All Plumbing", "Pipes"); selecting "Pipes" narrows to that product. The product list shows "Plumbing · Pipes".
- [ ] **Step 4: Push + PR** (stacked on PR #8)
```bash
git push -u origin feature/sub-categories
gh pr create --base main --head feature/sub-categories --title "Sub-categories (main -> subs)" --body "…"
```

---

## Self-review
- **Spec coverage:** parentId + migration (T1) ✓; CategoryTree helpers + tests (T2) ✓; PosProduct.mainCat + mapping + tree (T3) ✓; POS drill-down chips/filter (T4) ✓; Products tree + Main/Sub pickers + save(main,sub) + edit-load + drill-down + label (T5) ✓; migration + on-device (T6) ✓.
- **Placeholders:** none (T2 Step 2 superseded sketch removed inline; T6 PR body `…` filled at push).
- **Type consistency:** `Category.parentId: Long?`; `MainCat(id,name,subs)`; `buildCategoryTree`/`mainIdOf`/`matchesCategory`/`categoryLabel`; `PosProduct.mainCat`; `toCategoryTree()`; `categoryTree`; `saveProduct(mainCategoryName, subCategoryName, …)`; `categoryNamesFor(productId): Pair<String,String>` (suspend). Consistent across tasks.
- **Risk:** Task 5 Step 4 — confirm the Products chip Row's parent layout allows a sibling sub-Row (read lines 240-250 before editing); `MIGRATION_8_9` adds a nullable column (no default) so the entity (`parentId: Long? = null`) matches with no `@ColumnInfo`.
