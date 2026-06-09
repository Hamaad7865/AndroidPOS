# POS Reorderable Product Grid (Pillar A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the cashier arrange the POS product tiles into a saved custom order (pick a tile, tap where it goes), without affecting tap-to-add, scan, categories, or search.

**Architecture:** The order is a SharedPreferences list of product ids (no DB change). A pure `orderProducts` helper sorts the catalog by that order (rest A–Z). `PosSaleScreen` applies it before filtering and adds an "Arrange" mode with pick-and-place.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4. Spec: `docs/superpowers/specs/2026-06-09-pos-reorder-design.md`. Branch `feature/pos-reorder` (off main). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation.

---

## File structure
**Create:** `data/profile/PosLayoutPrefs.kt`, `ui/sale/ProductOrder.kt` + `test/.../ProductOrderTest.kt`.
**Modify:** `ui/sale/PosSaleScreen.kt`.

---

## Task 1: PosLayoutPrefs (order persistence)

**Files:** Create `app/src/main/java/com/nexapos/retail/data/profile/PosLayoutPrefs.kt`

- [ ] **Step 1: Implement** (mirrors `BusinessProfile`)
```kotlin
package com.nexapos.retail.data.profile

import android.content.Context

/** Persists the cashier's custom POS product-tile order as a list of product ids. */
object PosLayoutPrefs {
    private const val PREFS = "nexapos_layout"
    private const val KEY_ORDER = "product_order"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The saved id order (empty when the cashier hasn't arranged anything). */
    fun order(context: Context): List<String> =
        prefs(context).getString(KEY_ORDER, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    fun setOrder(
        context: Context,
        ids: List<String>,
    ) {
        prefs(context).edit().putString(KEY_ORDER, ids.joinToString("\n")).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ORDER).apply()
    }
}
```

- [ ] **Step 2: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/data/profile/PosLayoutPrefs.kt
git commit -m "feat: PosLayoutPrefs — persist custom POS product order"
```

---

## Task 2: orderProducts helper (TDD)

**Files:** Create `app/src/main/java/com/nexapos/retail/ui/sale/ProductOrder.kt`, `app/src/test/java/com/nexapos/retail/ui/sale/ProductOrderTest.kt`

- [ ] **Step 1: Write the failing tests**
```kotlin
package com.nexapos.retail.ui.sale

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductOrderTest {
    private fun p(
        id: String,
        name: String,
    ) = PosProduct(id = id, name = name, cat = "c", price = 1, sku = "", stock = 0, kind = "generic")

    @Test
    fun `empty order is alphabetical`() {
        val r = orderProducts(listOf(p("1", "Banana"), p("2", "Apple")), emptyList())
        assertEquals(listOf("Apple", "Banana"), r.map { it.name })
    }

    @Test
    fun `saved order leads, the rest follow alphabetically`() {
        val items = listOf(p("1", "Banana"), p("2", "Apple"), p("3", "Cherry"))
        val r = orderProducts(items, listOf("3", "1"))
        assertEquals(listOf("Cherry", "Banana", "Apple"), r.map { it.name })
    }

    @Test
    fun `ids not in the catalog are ignored`() {
        val r = orderProducts(listOf(p("1", "Banana"), p("2", "Apple")), listOf("99", "2"))
        assertEquals(listOf("Apple", "Banana"), r.map { it.name })
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*ProductOrderTest*" --rerun-tasks --no-build-cache` → FAIL (`orderProducts` unresolved).

- [ ] **Step 3: Implement**
```kotlin
package com.nexapos.retail.ui.sale

/**
 * Orders [products] by each product's position in [savedOrder] (ids not listed fall to the
 * end), breaking ties alphabetically by name. Empty [savedOrder] yields a pure A–Z order.
 */
fun orderProducts(
    products: List<PosProduct>,
    savedOrder: List<String>,
): List<PosProduct> {
    val rank = savedOrder.withIndex().associate { (i, id) -> id to i }
    return products.sortedWith(compareBy({ rank[it.id] ?: Int.MAX_VALUE }, { it.name.lowercase() }))
}
```

- [ ] **Step 4: Run to verify it passes** — same command → PASS.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/ProductOrder.kt app/src/test/java/com/nexapos/retail/ui/sale/ProductOrderTest.kt
git commit -m "feat: orderProducts helper (saved order then A-Z) + tests"
```

---

## Task 3: PosSaleScreen — apply order + Arrange mode

**Files:** Modify `app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Import** — add `import com.nexapos.retail.data.profile.PosLayoutPrefs` to the import block. (`orderProducts` is same-package — no import.)

- [ ] **Step 2: State** — right after `val c = PosTheme.colors` (top of `PosSaleScreen`, ≈ line 76), add:
```kotlin
    val reorderCtx = androidx.compose.ui.platform.LocalContext.current
    var order by remember { mutableStateOf(PosLayoutPrefs.order(reorderCtx)) }
    var arrangeMode by remember { mutableStateOf(false) }
    var pickedId by remember { mutableStateOf<String?>(null) }
```

- [ ] **Step 3: Apply the order in `visible`** — replace:
```kotlin
    val visible =
        vm.products.filter {
            matchesCategory(it.mainCat.ifEmpty { it.cat }, it.cat, selMain, selSub) &&
                (query.isBlank() || (it.name + " " + it.sku).contains(query, ignoreCase = true))
        }
```
with:
```kotlin
    val visible =
        orderProducts(vm.products, order).filter {
            matchesCategory(it.mainCat.ifEmpty { it.cat }, it.cat, selMain, selSub) &&
                (query.isBlank() || (it.name + " " + it.sku).contains(query, ignoreCase = true))
        }
```

- [ ] **Step 4: Arrange toggle in the search row** — replace the "New" button line (`SmallBtn(PosIcons.plus, "New") { vm.startNewTicket() }`, ≈ line 190) with:
```kotlin
                        SmallBtn(PosIcons.plus, "New") { vm.startNewTicket() }
                        SmallBtn(PosIcons.filter, if (arrangeMode) "Done" else "Arrange") {
                            arrangeMode = !arrangeMode
                            pickedId = null
                        }
```

- [ ] **Step 5: Arrange banner** — directly BEFORE the `// Product grid (4 columns, staggered reveal)` comment (≈ line 230) add:
```kotlin
                    if (arrangeMode) {
                        val pickedName = visible.firstOrNull { it.id == pickedId }?.name
                        Row(
                            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                if (pickedName != null) "Moving \"$pickedName\" — tap where it should go" else "Tap a product to move it",
                                fontSize = 12.sp,
                                color = c.amber,
                                fontWeight = FontWeight.SemiBold,
                            )
                            SmallBtn(PosIcons.trash, "Reset A–Z") {
                                PosLayoutPrefs.clear(reorderCtx)
                                order = emptyList()
                                pickedId = null
                            }
                        }
                    }
```
> If `PosIcons.trash` doesn't exist, use any present icon (e.g. `PosIcons.filter`). Confirm by reading the `PosIcons` object.

- [ ] **Step 6: Arrange-aware tap + highlight in the grid** — replace the `ProductCard(...)` call inside the grid:
```kotlin
                                    ProductCard(
                                        p = p,
                                        index = rowIdx * 4 + colIdx,
                                        onAdd = { coords -> add(p, coords) },
                                        modifier = Modifier.weight(1f),
                                    )
```
with:
```kotlin
                                    ProductCard(
                                        p = p,
                                        index = rowIdx * 4 + colIdx,
                                        picked = arrangeMode && pickedId == p.id,
                                        onAdd = { coords ->
                                            if (arrangeMode) {
                                                val picked = pickedId
                                                when {
                                                    picked == null -> pickedId = p.id
                                                    picked == p.id -> pickedId = null
                                                    else -> {
                                                        val fullIds = orderProducts(vm.products, order).map { it.id }
                                                        val without = fullIds.filterNot { it == picked }
                                                        val idx = without.indexOf(p.id).coerceAtLeast(0)
                                                        val newIds = without.toMutableList().also { it.add(idx, picked) }
                                                        PosLayoutPrefs.setOrder(reorderCtx, newIds)
                                                        order = newIds
                                                        pickedId = null
                                                    }
                                                }
                                            } else {
                                                add(p, coords)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
```

- [ ] **Step 7: ProductCard `picked` highlight** — add the param to `ProductCard` (≈ line 375):
```kotlin
private fun ProductCard(
    p: PosProduct,
    index: Int,
    onAdd: (LayoutCoordinates?) -> Unit,
    modifier: Modifier = Modifier,
    picked: Boolean = false,
) {
```
and change its border line (`.border(1.dp, c.hairline, RoundedCornerShape(14.dp))`) to:
```kotlin
                .border(if (picked) 2.dp else 1.dp, if (picked) c.amber else c.hairline, RoundedCornerShape(14.dp))
```

- [ ] **Step 8: Format, build, full check** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --continue --no-build-cache` → BUILD SUCCESSFUL. Fix any icon/import the compiler flags.
- [ ] **Step 9: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "feat: POS Arrange mode — pick-and-place custom product order"
```

---

## Task 4: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL; `ProductOrderTest` green.
- [ ] **Step 2: On-device** — `adb install -r app/build/outputs/apk/debug/app-debug.apk`; POS → tap **Arrange** → tap a product (it highlights) → tap another product → the first moves before it; **Done**; the new order shows. Reopen the app → order persists. **Reset A–Z** → alphabetical.
- [ ] **Step 3: Push + PR**
```bash
git push -u origin feature/pos-reorder
gh pr create --base main --head feature/pos-reorder --title "POS: reorderable product grid (Arrange mode)" --body "…"
```

---

## Self-review
- **Spec coverage:** PosLayoutPrefs order persistence (T1) ✓; orderProducts helper + tests (T2) ✓; apply order before filter + Arrange toggle + pick/place + Done/Reset + highlight (T3) ✓; verify (T4) ✓.
- **Placeholders:** none (T3 Step 5/PR body note an icon-name confirmation, not a logic gap).
- **Type consistency:** `PosLayoutPrefs.order/setOrder/clear`; `orderProducts(products, savedOrder)`; `ProductCard(..., picked)`; screen state `order`/`arrangeMode`/`pickedId`/`reorderCtx`. Consistent across tasks.
- **Risk:** confirm `PosIcons.filter`/`PosIcons.trash` exist (read `PosIcons`); the arrange tap reuses `onAdd`, so the tile pop-animation still fires on tap in arrange mode (harmless feedback).
