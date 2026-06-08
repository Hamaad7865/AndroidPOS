# Discount Dialog (cart + item) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A checkout discount **dialog** with a Cart-discount tab and an Item-discount tab (each % or Rs), correct totals, VAT recomputed on the discounted amount, and per-line discounts persisted + printed.

**Architecture:** A per-line `PosLine.discount` (flat Rs) + a persisted `SaleItem.discountCents` (non-destructive v7→v8 migration) feed a reworked `SellingViewModel` calc (`afterItems`, `totalDiscount`, `total`, pure `discountedVat`). The inline discount field is replaced by a "Discount" row that opens a `DiscountDialog`.

**Tech Stack:** Kotlin, Jetpack Compose, Room (SQLCipher), JUnit4. Spec: `docs/superpowers/specs/2026-06-09-discount-dialog-design.md`. Branch `feature/pos-discount` (stacked on `feature/vat-types`). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation; add `--rerun-tasks --no-build-cache` to `testDebugUnitTest` when results look stale.

---

## File structure
**Create:** `MIGRATION_7_8` (in `PosDatabase.kt`); `discountedVat` (in `VatCalc.kt`) + tests; `DiscountDialog`/tabs (in `PosCheckoutScreen.kt`).
**Modify:** `data/entity/SaleItem.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`, `ui/sale/SellingViewModel.kt`, `ui/sale/VatCalc.kt`, `ui/checkout/PosCheckoutScreen.kt`, `ui/checkout/PosReceiptScreen.kt`, `ui/checkout/ReceiptOutput.kt`.

---

## Task 1: SaleItem.discountCents + non-destructive migration v7→v8

**Files:** Modify `data/entity/SaleItem.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`

- [ ] **Step 1: Add the column** — in `SaleItem.kt`, add `import androidx.room.ColumnInfo`, and after `val lineTotalCents: Long,` add:
```kotlin
    /** Flat Rs discount applied to this line (minor units). Default 0 — declared so the
     *  v7→v8 migration's column default matches Room's expected schema (no destructive wipe). */
    @ColumnInfo(defaultValue = "0")
    val discountCents: Long = 0,
```

- [ ] **Step 2: Migration + version bump** — in `PosDatabase.kt`, change `version = 7,` to `version = 8,`, add the comment line `// v8: sale_items gained a discountCents column (additive, non-destructive migration MIGRATION_7_8).` above it, and add below the existing `MIGRATION_6_7` block:
```kotlin

/** v7→v8: add sale_items.discountCents, defaulting existing rows to 0 (no line discount). */
val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sale_items ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0")
        }
    }
```

- [ ] **Step 3: Register it** — in `AppContainer.kt`, add `import com.nexapos.retail.data.MIGRATION_7_8`, and change `.addMigrations(MIGRATION_6_7)` to `.addMigrations(MIGRATION_6_7, MIGRATION_7_8)`.

- [ ] **Step 4: Build + check schema** — `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug` → BUILD SUCCESSFUL. Then confirm the exported schema has the default:
  `find app -name "8.json" -path "*PosDatabase*" | head -1 | xargs grep -o 'discountCents` INTEGER NOT NULL DEFAULT 0' ` → prints a match.

- [ ] **Step 5: Commit** (include the generated `app/schemas/.../8.json`)
```bash
git add app/src/main/java/com/nexapos/retail/data/entity/SaleItem.kt app/src/main/java/com/nexapos/retail/data/PosDatabase.kt app/src/main/java/com/nexapos/retail/di/AppContainer.kt app/schemas/com.nexapos.retail.data.PosDatabase/8.json
git commit -m "feat: sale_items.discountCents column + non-destructive migration v7->v8"
```

---

## Task 2: PosLine per-line discount

**Files:** Modify `ui/sale/SellingViewModel.kt`

- [ ] **Step 1: Add the field + net** — replace the `PosLine` declaration:
```kotlin
data class PosLine(val product: PosProduct, val qty: Int) {
    val lineTotal get() = product.price * qty
}
```
with:
```kotlin
data class PosLine(
    val product: PosProduct,
    val qty: Int,
    val discount: Int = 0,
) {
    val lineTotal get() = product.price * qty

    /** Line amount after its own discount (never negative). */
    val net get() = (lineTotal - discount).coerceAtLeast(0)
}
```

- [ ] **Step 2: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt
git commit -m "feat: per-line discount on PosLine"
```

---

## Task 3: discountedVat helper (TDD)

**Files:** Modify `ui/sale/VatCalc.kt`, `test/.../VatCalcTest.kt`

- [ ] **Step 1: Write the failing tests** — add to `app/src/test/java/com/nexapos/retail/ui/sale/VatCalcTest.kt` (inside the existing `class VatCalcTest`, reusing its `product`/`line` helpers from `Discount* `… if absent, add them):
```kotlin
    private fun stdProduct(price: Int) =
        PosProduct(id = "1", name = "x", cat = "c", price = price, sku = "s", stock = 0, kind = "generic", vatType = com.nexapos.retail.data.entity.VatType.STANDARD)

    @org.junit.Test
    fun `discountedVat with no discount equals plain inclusive vat`() {
        val lines = listOf(PosLine(stdProduct(1150), 1))
        assertEquals(150, discountedVat(lines, cartDiscount = 0, vatRegistered = true))
    }

    @org.junit.Test
    fun `item discount lowers the vat`() {
        // net = 1150 - 150 = 1000 → vat = 1000 - 1000/1.15 = 130
        val lines = listOf(PosLine(stdProduct(1150), 1, discount = 150))
        assertEquals(130, discountedVat(lines, cartDiscount = 0, vatRegistered = true))
    }

    @org.junit.Test
    fun `cart discount lowers the vat proportionally`() {
        // afterItems = 1150, cart 150 → final 1000 → vat 130
        val lines = listOf(PosLine(stdProduct(1150), 1))
        assertEquals(130, discountedVat(lines, cartDiscount = 150, vatRegistered = true))
    }

    @org.junit.Test
    fun `discountedVat is zero when not registered`() {
        assertEquals(0, discountedVat(listOf(PosLine(stdProduct(1150), 1)), cartDiscount = 0, vatRegistered = false))
    }
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*VatCalcTest*" --rerun-tasks --no-build-cache` → FAIL (`discountedVat` unresolved).

- [ ] **Step 3: Implement** — add to `VatCalc.kt`:
```kotlin
/**
 * VAT embedded in the cart AFTER discounts. Standard-rated lines carry the 15% inside
 * their (item-discounted) net; the cart discount is spread proportionally across that net.
 * Returns 0 when the business is not VAT-registered.
 */
fun discountedVat(
    lines: List<PosLine>,
    cartDiscount: Int,
    vatRegistered: Boolean,
): Int {
    if (!vatRegistered) return 0
    val afterItems = lines.sumOf { it.net }
    if (afterItems <= 0) return 0
    val standardNet = lines.filter { it.product.vatType == VatType.STANDARD }.sumOf { it.net }
    val cartRatio = (afterItems - cartDiscount).coerceAtLeast(0).toDouble() / afterItems
    val standardFinal = standardNet * cartRatio
    return (standardFinal - standardFinal / STANDARD_DIVISOR).roundToInt()
}
```
(`STANDARD_DIVISOR` = 1.15 already exists in this file.)

- [ ] **Step 4: Run to verify it passes** — same command → PASS.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/VatCalc.kt app/src/test/java/com/nexapos/retail/ui/sale/VatCalcTest.kt
git commit -m "feat: discountedVat — VAT on the discounted amount (tested)"
```

---

## Task 4: SellingViewModel calc rework

**Files:** Modify `ui/sale/SellingViewModel.kt`

- [ ] **Step 1: Replace the checkout getters** — replace this block:
```kotlin
    val subtotal get() = workingLines.sumOf { it.lineTotal }

    /** VAT embedded in the cart — per product VAT type, gated on the business being VAT-registered. */
    val vat get() = vatOf(workingLines, vatRegistered)

    /**
     * Discount is clamped so it can never exceed the subtotal (prevents a free sale
     * slipping through by a large manual discount entry).
     */
    private val clampedDiscount get() = discount.coerceIn(0, subtotal)

    /** Exact payable total — prices are VAT-inclusive, so VAT is NOT added again. */
    val total get() = (subtotal - clampedDiscount + shipping).coerceAtLeast(0)
```
with:
```kotlin
    val subtotal get() = workingLines.sumOf { it.lineTotal }

    /** Total of the per-line (item) discounts. */
    val itemDiscountTotal get() = workingLines.sumOf { it.discount }

    /** Subtotal after item discounts; the base the cart discount applies to. */
    val afterItems get() = (subtotal - itemDiscountTotal).coerceAtLeast(0)

    /** Cart discount, clamped so it can never exceed the after-item subtotal. */
    private val clampedDiscount get() = discount.coerceIn(0, afterItems)

    /** Item discounts + cart discount — shown as the single "Discount" figure. */
    val totalDiscount get() = itemDiscountTotal + clampedDiscount

    /** VAT embedded in the cart after all discounts, per product VAT type. */
    val vat get() = discountedVat(workingLines, clampedDiscount, vatRegistered)

    /** Exact payable total — prices are VAT-inclusive, so VAT is NOT added again. */
    val total get() = (afterItems - clampedDiscount + shipping).coerceAtLeast(0)
```

- [ ] **Step 2: Cart % now uses afterItems** — in `applyDiscount`, change `discount = percentToFlat(subtotal, discountPercent)` to `discount = percentToFlat(afterItems, discountPercent)`.

- [ ] **Step 3: Add item-discount + clear + restore actions** — add after `applyDiscount`:
```kotlin
    /** Sets a line's discount from a percentage of its line total or a flat Rs amount, clamped to the line. */
    fun applyItemDiscount(
        productId: String,
        isPercent: Boolean,
        value: Int,
    ) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i < 0) return
        val line = workingLines[i]
        val flat = if (isPercent) percentToFlat(line.lineTotal, value) else value.coerceAtLeast(0)
        workingLines[i] = line.copy(discount = flat.coerceIn(0, line.lineTotal))
        if (!isCredit) received = total
    }

    /** Removes every discount — cart and per-line. */
    fun clearAllDiscounts() {
        discount = 0
        discountIsPercent = false
        discountPercent = 0
        for (i in workingLines.indices) {
            if (workingLines[i].discount != 0) workingLines[i] = workingLines[i].copy(discount = 0)
        }
        if (!isCredit) received = total
    }

    /** Restores a captured discount state (used by the dialog's Cancel). */
    fun restoreDiscounts(
        cartDiscount: Int,
        cartIsPercent: Boolean,
        cartPercent: Int,
        lineDiscounts: Map<String, Int>,
    ) {
        discount = cartDiscount
        discountIsPercent = cartIsPercent
        discountPercent = cartPercent
        for (i in workingLines.indices) {
            val d = lineDiscounts[workingLines[i].product.id] ?: 0
            if (workingLines[i].discount != d) workingLines[i] = workingLines[i].copy(discount = d)
        }
        if (!isCredit) received = total
    }
```

- [ ] **Step 4: Clear line discounts on ticket/checkout reset** — in `startNewTicket()` and `beginCheckout()`, after the existing `discountPercent = 0` line, add a loop:
```kotlin
        for (i in workingLines.indices) {
            if (workingLines[i].discount != 0) workingLines[i] = workingLines[i].copy(discount = 0)
        }
```
(In `startNewTicket()` this runs before `workingLines.clear()` would be redundant — place it is fine either way; keep it for `beginCheckout()` which does NOT clear the cart.)

- [ ] **Step 5: Persist the line discount** — in `persist()`, in the `SaleItem(...)` builder add after `lineTotalCents = line.lineTotal * CENTS_PER_RUPEE,`:
```kotlin
                    discountCents = line.discount * CENTS_PER_RUPEE,
```

- [ ] **Step 6: Build + tests** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --rerun-tasks --no-build-cache`. **Note:** `SellingViewModelTest`'s VAT assertion may shift by ≤1 rupee because `vat` now uses total-rounding (`discountedVat`) instead of per-line rounding. If a VAT assertion fails purely on that, update its expected value to the new figure with a comment `// VAT now rounded on the discounted total`. All other assertions must still pass.

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt app/src/test/java/com/nexapos/retail/ui/sale/SellingViewModelTest.kt
git commit -m "feat: cart+item discount calc, discounted VAT, persist line discount"
```

---

## Task 5: Discount row + DiscountDialog

**Files:** Modify `ui/checkout/PosCheckoutScreen.kt`

- [ ] **Step 1: Replace the inline DiscountField with a Discount row that opens the dialog** — in `BillCard`, replace the "charges" `Row { DiscountField(...) ; NumField("Shipping" …) }` block with a discount trigger row + the shipping field + the dialog:
```kotlin
            // charges
            var showDiscount by remember { mutableStateOf(false) }
            if (showDiscount) DiscountDialog(vm) { showDiscount = false }
            Column(
                Modifier.fillMaxWidth().background(c.raised2).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.raised)
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                        .clickable { showDiscount = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Discount", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text(
                        if (vm.totalDiscount > 0) "− " + rs(vm.totalDiscount) else "Add discount",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (vm.totalDiscount > 0) c.ink else c.amber,
                    )
                }
                NumField(
                    label = "Shipping",
                    value = vm.shipping,
                    onChange = { v ->
                        vm.shipping = v
                        if (!vm.isCredit) vm.received = vm.total
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
```
(`rs(...)` is `PosCheckoutScreen`'s private money formatter; `clickable` is already imported.)

- [ ] **Step 2: Replace the old `DiscountField` composable with `DiscountDialog` + tabs** — delete the existing `DiscountField` composable and add (keep the existing `ModeChip`):
```kotlin
@Composable
private fun DiscountDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    // Snapshot for Cancel/revert.
    val snapCart = remember { vm.discount }
    val snapIsPct = remember { vm.discountIsPercent }
    val snapPct = remember { vm.discountPercent }
    val snapLines = remember { vm.workingLines.associate { it.product.id to it.discount } }
    var tab by remember { mutableStateOf(0) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.TextButton(onClick = { vm.clearAllDiscounts() }) { Text("Clear") }
                androidx.compose.material3.TextButton(onClick = {
                    vm.restoreDiscounts(snapCart, snapIsPct, snapPct, snapLines)
                    onDismiss()
                }) { Text("Cancel") }
            }
        },
        title = { Text("Discount") },
        text = {
            Column(Modifier.widthIn(min = 360.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabChip("Cart discount", tab == 0, Modifier.weight(1f)) { tab = 0 }
                    TabChip("Item discount", tab == 1, Modifier.weight(1f)) { tab = 1 }
                }
                Spacer(Modifier.height(12.dp))
                if (tab == 0) CartDiscountTab(vm) else ItemDiscountTab(vm)
            }
        },
    )
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.ink else c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) c.surface else c.ink)
    }
}

@Composable
private fun CartDiscountTab(vm: SellingViewModel) {
    val c = PosTheme.colors
    val isPct = vm.discountIsPercent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Apply cart discount", fontSize = 12.sp, color = c.muted)
            Row(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, c.hairline, RoundedCornerShape(7.dp))) {
                ModeChip("%", isPct) { vm.applyDiscount(isPercent = true, value = vm.discountPercent) }
                ModeChip("Rs", !isPct) { vm.applyDiscount(isPercent = false, value = vm.discount) }
            }
        }
        DiscountInputRow(isPct = isPct, value = if (isPct) vm.discountPercent else vm.discount) { v ->
            vm.applyDiscount(isPercent = isPct, value = v)
        }
        Divider()
        SummaryRow("Subtotal", rs(vm.subtotal))
        SummaryRow("Total discount", "− " + rs(vm.totalDiscount))
        SummaryRow("Total", rs(vm.total), bold = true)
    }
}

@Composable
private fun ItemDiscountTab(vm: SellingViewModel) {
    val c = PosTheme.colors
    Column(
        Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        vm.workingLines.forEach { line ->
            var pct by remember(line.product.id) { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.raised)
                    .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.product.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(rs(line.lineTotal), fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.muted)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, c.hairline, RoundedCornerShape(7.dp))) {
                        ModeChip("%", pct) { pct = true }
                        ModeChip("Rs", !pct) { pct = false }
                    }
                    DiscountInputRow(isPct = pct, value = line.discount.takeIf { !pct } ?: 0, modifier = Modifier.weight(1f)) { v ->
                        vm.applyItemDiscount(line.product.id, isPercent = pct, value = v)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscountInputRow(
    isPct: Boolean,
    value: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    val c = PosTheme.colors
    Row(
        modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!isPct) Text("Rs", fontSize = 13.sp, color = c.muted)
        BasicTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { onChange(it.toIntOrNull() ?: 0) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(c.amber),
        )
        if (isPct) Text("%", fontSize = 13.sp, color = c.muted)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) c.ink else c.muted)
        Text(value, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = c.ink)
    }
}
```
> Imports already present in this file: `BasicTextField`, `KeyboardOptions`, `KeyboardType`, `TextStyle`, `SolidColor`, `TextAlign`, `RoundedCornerShape`, `verticalScroll`, `rememberScrollState`, `heightIn`, `widthIn`, `clickable`, `TextOverflow` (used by the cart line list), `JetBrainsMono`. `Divider` is a private composable already in this file. `ModeChip` is kept from the earlier inline work.

- [ ] **Step 3: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL. (If any symbol above is not imported, add the import.)
- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/checkout/PosCheckoutScreen.kt
git commit -m "feat: discount dialog (cart + item tabs) replaces inline discount field"
```

---

## Task 6: Per-line + total discount on the receipt

**Files:** Modify `ui/checkout/PosReceiptScreen.kt`, `ui/checkout/ReceiptOutput.kt`

- [ ] **Step 1: On-screen receipt** — in `PosReceiptScreen.kt` `ReceiptPaper`, in the `sale.lines.forEach { l -> … }` loop, after the qty/price row add a per-line discount line when present:
```kotlin
                if (l.discount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("− ${formatNum(l.discount.toDouble(), 0)} disc", fontFamily = JetBrainsMono, fontSize = 10.sp, color = receiptMuted)
                    }
                }
```
The existing `RcMoney("Discount", sale.discount)` already shows the cart discount; change it to the total discount by computing it from the snapshot lines: replace `RcMoney("Discount", sale.discount)` with
```kotlin
            val itemDisc = sale.lines.sumOf { it.discount }
            if (sale.discount + itemDisc > 0) RcMoney("Discount", sale.discount + itemDisc)
```

- [ ] **Step 2: PDF + print + text receipts** — in `ReceiptOutput.kt`:
  - `renderPdf`: after the `row("   ${l.qty} × …", …)` per-line call inside `sale.lines.forEach`, add `if (l.discount > 0) row("   − disc", p.qty, "-" + formatNum(l.discount.toDouble(), 0), p.amtR)`.
  - `renderPdf` summary + `html` + `messageText`: where the cart discount is shown (`if (sale.discount > 0) … Discount …`), change the amount to `sale.discount + sale.lines.sumOf { it.discount }` and the condition to `> 0` on that sum (so the total discount prints). In `html`, the discount `<tr>` is `${if (sale.discount > 0) "…Discount…${money(sale.discount)}…" else ""}` — change to use the summed total.

- [ ] **Step 3: Format, build, lint** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck`.
- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/checkout/PosReceiptScreen.kt app/src/main/java/com/nexapos/retail/ui/checkout/ReceiptOutput.kt
git commit -m "feat: show per-line + total discount on every receipt surface"
```

---

## Task 7: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL.
- [ ] **Step 2: Migration test (v7→v8 preserves data)** — install the build over the existing v7 emulator DB (which holds a product/sale): `adb install -r app/build/outputs/apk/debug/app-debug.apk`, relaunch, sign in → the product/sales are still present (non-destructive upgrade).
- [ ] **Step 3: On-device feature test** — build a cart, open Checkout → tap the **Discount** row → **Cart discount** tab: % and Rs apply, summary updates; **Item discount** tab: discount one line; **Clear** zeroes everything; **Cancel** reverts to the pre-open state; **OK** keeps. Complete the sale → the receipt shows per-line + total discount.
- [ ] **Step 4: Push + PR** (stacked on PR #7)
```bash
git push -u origin feature/pos-discount
gh pr create --base main --head feature/pos-discount --title "POS discount dialog (cart + item discounts)" --body "…"
```

---

## Self-review
- **Spec coverage:** SaleItem.discountCents + migration (T1) ✓; PosLine.discount/net (T2) ✓; discountedVat + tests (T3) ✓; afterItems/totalDiscount/total/cart-%-on-afterItems/applyItemDiscount/clearAllDiscounts/restoreDiscounts/persist/resets (T4) ✓; Discount row + DiscountDialog cart+item tabs + Clear/Cancel/OK (T5) ✓; receipt per-line + total discount across on-screen/PDF/print/text (T6) ✓; migration + on-device verification (T7) ✓.
- **Placeholders:** none (T7 PR body `…` filled at push; T6 Step 2 describes the exact same edit pattern already proven in the VAT feature for these three render sites).
- **Type consistency:** `PosLine(product, qty, discount)` + `.net`; `discountedVat(lines, cartDiscount, vatRegistered)`; `applyItemDiscount(productId, isPercent, value)`, `clearAllDiscounts()`, `restoreDiscounts(cartDiscount, cartIsPercent, cartPercent, lineDiscounts)`; `totalDiscount`/`afterItems`/`itemDiscountTotal`; `MIGRATION_7_8`; `SaleItem.discountCents`. Consistent across tasks.
- **Risk flagged:** T4 Step 6 — `SellingViewModelTest` VAT figure may move ≤1 rupee (total vs per-line rounding); update that one assertion if so.
