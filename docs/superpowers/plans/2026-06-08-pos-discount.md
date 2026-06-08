# POS Discount (% or Rs + presets) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the cashier enter the checkout discount as a percentage or a flat Rs amount, with one-tap 5/10/15% presets, keeping the stored discount a flat Rs figure.

**Architecture:** `SellingViewModel.discount` stays the flat-Rs source of truth for the total; a `discountIsPercent`/`discountPercent` pair plus `applyDiscount()` compute it. A pure `percentToFlat` helper is unit-tested. A `DiscountField` composable (toggle + field + preset chips) replaces the discount number field in the Checkout "charges" area.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4. Spec: `docs/superpowers/specs/2026-06-08-pos-discount-design.md`. Branch `feature/pos-discount`. Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; run `ktlintFormat` in its own invocation before checks. (If `testDebugUnitTest` returns stale results, add `--rerun-tasks --no-build-cache`.)

---

## File structure
**Create:** `ui/sale/DiscountCalc.kt`, `test/.../DiscountCalcTest.kt`.
**Modify:** `ui/sale/SellingViewModel.kt`, `ui/checkout/PosCheckoutScreen.kt`, `ui/sale/PosSaleScreen.kt`.

---

## Task 1: percentToFlat helper (TDD)

**Files:** Create `app/src/main/java/com/nexapos/retail/ui/sale/DiscountCalc.kt`, `app/src/test/java/com/nexapos/retail/ui/sale/DiscountCalcTest.kt`

- [ ] **Step 1: Write the failing test**
```kotlin
package com.nexapos.retail.ui.sale

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscountCalcTest {
    @Test
    fun `ten percent of 1000 is 100`() {
        assertEquals(100, percentToFlat(1000, 10))
    }

    @Test
    fun `rounds to the nearest rupee`() {
        // 1455 * 7% = 101.85 → 102
        assertEquals(102, percentToFlat(1455, 7))
    }

    @Test
    fun `percent above 100 is clamped`() {
        assertEquals(1000, percentToFlat(1000, 150))
    }

    @Test
    fun `negative percent clamps to zero`() {
        assertEquals(0, percentToFlat(1000, -5))
    }
}
```

- [ ] **Step 2: Run it to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*DiscountCalcTest*"` → FAIL (`percentToFlat` unresolved).

- [ ] **Step 3: Write the implementation**
```kotlin
package com.nexapos.retail.ui.sale

import kotlin.math.roundToInt

/** Flat-rupee discount equal to [pct]% of [subtotal]. Percent is clamped to 0..100. */
fun percentToFlat(
    subtotal: Int,
    pct: Int,
): Int = (subtotal * pct.coerceIn(0, 100) / 100.0).roundToInt()
```

- [ ] **Step 4: Run tests to verify they pass** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*DiscountCalcTest*"` → PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/DiscountCalc.kt app/src/test/java/com/nexapos/retail/ui/sale/DiscountCalcTest.kt
git commit -m "feat: percentToFlat helper for percentage discounts"
```

---

## Task 2: SellingViewModel discount mode + applyDiscount

**Files:** Modify `app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt`

- [ ] **Step 1: Add the mode state** — after the `var received by mutableStateOf(0)` line (in the "Checkout inputs" block) add:
```kotlin
    var discountIsPercent by mutableStateOf(false)
    var discountPercent by mutableStateOf(0)
```

- [ ] **Step 2: Add `applyDiscount`** — immediately after the `startNewTicket()` function add:
```kotlin
    /**
     * Sets the discount from either a percentage of the subtotal or a flat Rs amount.
     * Stores the resulting flat [discount] (the source of truth for the total) and keeps
     * the cash tender in sync, mirroring direct edits to the field.
     */
    fun applyDiscount(
        isPercent: Boolean,
        value: Int,
    ) {
        discountIsPercent = isPercent
        if (isPercent) {
            discountPercent = value.coerceIn(0, 100)
            discount = percentToFlat(subtotal, discountPercent)
        } else {
            discount = value.coerceAtLeast(0)
        }
        if (!isCredit) received = total
    }
```

- [ ] **Step 3: Reset the mode in `startNewTicket`** — in `startNewTicket()`, change the `discount = 0` line block to also reset the mode. Replace:
```kotlin
        discount = 0
        shipping = 0
```
with:
```kotlin
        discount = 0
        discountIsPercent = false
        discountPercent = 0
        shipping = 0
```

- [ ] **Step 4: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt
git commit -m "feat: discount percent mode + applyDiscount in SellingViewModel"
```

---

## Task 3: DiscountField composable (toggle + field + presets)

**Files:** Modify `app/src/main/java/com/nexapos/retail/ui/checkout/PosCheckoutScreen.kt`

- [ ] **Step 1: Add the import** (top import block): `import androidx.compose.foundation.layout.RowScope`.

- [ ] **Step 2: Replace the "charges" Row with a Column** — replace this block (the discount + shipping Row):
```kotlin
            // charges
            Row(
                Modifier.fillMaxWidth().background(c.raised2).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NumField(
                    label = "Discount (flat Rs)",
                    value = vm.discount,
                    onChange = { v ->
                        vm.discount = v
                        // Keep tender in sync if the cashier hasn't switched to credit.
                        if (!vm.isCredit) vm.received = vm.total
                    },
                    modifier = Modifier.weight(1f),
                )
                NumField(
                    label = "Shipping",
                    value = vm.shipping,
                    onChange = { v ->
                        vm.shipping = v
                        if (!vm.isCredit) vm.received = vm.total
                    },
                    modifier = Modifier.weight(1f),
                )
            }
```
with:
```kotlin
            // charges
            Column(
                Modifier.fillMaxWidth().background(c.raised2).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DiscountField(vm, Modifier.fillMaxWidth())
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

- [ ] **Step 3: Add the `DiscountField` + chip composables** — add these next to `NumField` (e.g. just below the `NumField` composable near the end of the file):
```kotlin
@Composable
private fun DiscountField(
    vm: SellingViewModel,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    val isPct = vm.discountIsPercent
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Discount", fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
            Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, c.hairline, RoundedCornerShape(8.dp))) {
                ModeChip("%", isPct) { vm.applyDiscount(isPercent = true, value = vm.discountPercent) }
                ModeChip("Rs", !isPct) { vm.applyDiscount(isPercent = false, value = vm.discount) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!isPct) Text("Rs", fontSize = 13.sp, color = c.muted)
            val shown = if (isPct) vm.discountPercent else vm.discount
            BasicTextField(
                value = if (shown == 0) "" else shown.toString(),
                onValueChange = { vm.applyDiscount(isPercent = isPct, value = it.toIntOrNull() ?: 0) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(c.amber),
            )
            if (isPct) Text("%", fontSize = 13.sp, color = c.muted)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(5, 10, 15).forEach { p ->
                PresetChip("$p%") { vm.applyDiscount(isPercent = true, value = p) }
            }
            PresetChip("Clear") { vm.applyDiscount(isPercent = false, value = 0) }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.ink else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) c.surface else c.ink)
    }
}

@Composable
private fun RowScope.PresetChip(
    label: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier
            .weight(1f)
            .height(30.dp)
            .clip(CircleShape)
            .background(c.raised)
            .border(1.dp, c.hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
    }
}
```
> All Compose symbols used here (`BasicTextField`, `KeyboardOptions`, `KeyboardType`, `TextStyle`, `SolidColor`, `TextAlign`, `CircleShape`, `RoundedCornerShape`, `Color`, `JetBrainsMono`, `em`, `sp`, `dp`) are already imported in this file (used by `NumField` and the quick-amount pills); only `RowScope` (Step 1) is new.

- [ ] **Step 4: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/checkout/PosCheckoutScreen.kt
git commit -m "feat: checkout DiscountField with % / Rs toggle and presets"
```

---

## Task 4: Wire the POS live-ticket Discount row

**Files:** Modify `app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Show the real discount** — replace:
```kotlin
                        TotalRow("Discount", "— Rs 0", true)
```
with:
```kotlin
                        TotalRow("Discount", if (vm.discount > 0) "— " + rs(vm.discount) else "Rs 0", true)
```

- [ ] **Step 2: Format, build, lint** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck` → BUILD SUCCESSFUL. Fix any detekt findings.
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "fix: show the real discount on the POS live ticket"
```

---

## Task 5: Full verification

- [ ] **Step 1: Full check suite** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue` (add `--rerun-tasks --no-build-cache` if results look stale) → BUILD SUCCESSFUL; `DiscountCalcTest` green.
- [ ] **Step 2: Manual on emulator** — install; build a cart, open Checkout: tap **10%** → discount = 10% of the subtotal, total + Received/Change update; toggle to **Rs** → type a flat amount; tap **Clear** → discount 0; the POS live-ticket Discount row reflects the value.
- [ ] **Step 3: Push + PR** (note it is stacked on PR #7)
```bash
git push -u origin feature/pos-discount
gh pr create --base main --head feature/pos-discount --title "POS discount: % / Rs toggle + quick presets" --body "…"
```

---

## Self-review
- **Spec coverage:** percentToFlat + tests (T1) ✓; VM mode state + applyDiscount + startNewTicket reset (T2) ✓; DiscountField toggle + field + 5/10/15%/Clear presets replacing the discount NumField (T3) ✓; POS live-ticket Discount row wired (T4) ✓; clamps (pct 0–100 in `applyDiscount`/`percentToFlat`; discount→subtotal unchanged in `clampedDiscount`) ✓; tender re-sync mirrors the old field (T2) ✓.
- **Placeholders:** none (T5 PR body `…` filled at push time).
- **Type consistency:** `applyDiscount(isPercent: Boolean, value: Int)`, `discountIsPercent`/`discountPercent`, `percentToFlat(subtotal, pct)`, `DiscountField(vm, modifier)`, `ModeChip(label, selected, onClick)`, `RowScope.PresetChip(label, onClick)` used consistently across tasks.
