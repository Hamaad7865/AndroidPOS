# POS Action Bar (Pillar B1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a POS Actions bar (Item lookup, Price override, Reprint, Returns, Hold, Void, Exit) to the ticket panel, with a per-line price override.

**Architecture:** `PosLine` gains an optional `priceOverride`; the line total and persisted sale use the effective price. `PosSaleScreen` replaces the Clear/Hold row with an Actions grid + two dialogs (Item lookup, Price override) and a Void confirm. No DB change.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4. Spec: `docs/superpowers/specs/2026-06-09-pos-actions-design.md`. Branch `feature/pos-actions` (stacked on `feature/pos-reorder`). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation.

---

## File structure
**Modify:** `ui/sale/SellingViewModel.kt` (PosLine.priceOverride + setLinePrice + persist), `ui/sale/PosSaleScreen.kt` (Actions grid + dialogs), `test/.../SellingViewModelTest.kt`.

---

## Task 1: Per-line price override (TDD)

**Files:** Modify `ui/sale/SellingViewModel.kt`, `test/.../SellingViewModelTest.kt`

- [ ] **Step 1: Write the failing test** — add to `SellingViewModelTest`:
```kotlin
    @Test
    fun `price override changes the line total and persisted unit price`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" } // catalog price 185
            model.addToCart(wrench)
            model.setLinePrice(wrench.id, 200)
            assertEquals(200, model.subtotal)
            model.beginCheckout()
            model.complete()
            val (_, items) = sales.recorded.first()
            assertEquals(200 * 100L, items.first().unitPriceCents)
            assertEquals(200 * 100L, items.first().lineTotalCents)
        }
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*SellingViewModelTest*" --rerun-tasks --no-build-cache` → FAIL (`setLinePrice` unresolved).

- [ ] **Step 3: PosLine + effectivePrice** — replace the `PosLine` declaration:
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
with:
```kotlin
data class PosLine(
    val product: PosProduct,
    val qty: Int,
    val discount: Int = 0,
    /** Custom unit price for this sale only; null = the catalog price. */
    val priceOverride: Int? = null,
) {
    val effectivePrice get() = priceOverride ?: product.price
    val lineTotal get() = effectivePrice * qty

    /** Line amount after its own discount (never negative). */
    val net get() = (lineTotal - discount).coerceAtLeast(0)
}
```

- [ ] **Step 4: setLinePrice** — add after `applyItemDiscount` (or near the other line actions) in `SellingViewModel`:
```kotlin
    /** Overrides a line's unit price for this sale (catalog price untouched). */
    fun setLinePrice(
        productId: String,
        priceRupees: Int,
    ) {
        val i = workingLines.indexOfFirst { it.product.id == productId }
        if (i < 0) return
        workingLines[i] = workingLines[i].copy(priceOverride = priceRupees.coerceAtLeast(0))
        if (!isCredit) received = total
    }
```

- [ ] **Step 5: persist uses the effective price** — in `persist()`, change `unitPriceCents = line.product.price * CENTS_PER_RUPEE,` to:
```kotlin
                    unitPriceCents = line.effectivePrice * CENTS_PER_RUPEE,
```
(`lineTotalCents = line.lineTotal * CENTS_PER_RUPEE` already uses the effective price.)

- [ ] **Step 6: Run to verify it passes** — same command → PASS (whole suite).
- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt app/src/test/java/com/nexapos/retail/ui/sale/SellingViewModelTest.kt
git commit -m "feat: per-line price override (effective price drives total + persisted sale)"
```

---

## Task 2: Actions bar + dialogs (PosSaleScreen)

**Files:** Modify `ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Imports** — ensure present (add any missing): `androidx.compose.foundation.text.BasicTextField`, `androidx.compose.foundation.text.KeyboardOptions`, `androidx.compose.ui.text.TextStyle`, `androidx.compose.ui.text.input.KeyboardType`, `androidx.compose.ui.text.style.TextAlign`, `androidx.compose.ui.graphics.SolidColor`, `androidx.compose.foundation.layout.widthIn`, `androidx.compose.foundation.layout.heightIn`, `androidx.compose.material3.AlertDialog`, `androidx.compose.material3.TextButton`. (Read the import block first; many exist.)

- [ ] **Step 2: Dialog state** — near the top of `PosSaleScreen` (with the other `remember`s, ≈ after `var pickedId`), add:
```kotlin
    var showLookup by remember { mutableStateOf(false) }
    var showPrice by remember { mutableStateOf(false) }
    var showVoid by remember { mutableStateOf(false) }
    val actionCtx = androidx.compose.ui.platform.LocalContext.current
```

- [ ] **Step 3: Replace the Clear/Hold row with the Actions grid** — replace:
```kotlin
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton("Clear", Modifier.weight(1f)) { vm.clearCart() }
                            SecondaryButton(
                                label = if (lines.isEmpty()) "Hold" else "Hold ticket",
                                modifier = Modifier.weight(1f),
                            ) { vm.holdCurrentTicket() }
                        }
```
with:
```kotlin
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionBtn("Lookup", Modifier.weight(1f)) { showLookup = true }
                                ActionBtn("Price", Modifier.weight(1f)) { showPrice = true }
                                ActionBtn("Reprint", Modifier.weight(1f)) {
                                    if (vm.lastSale != null) {
                                        onNav("receipt")
                                    } else {
                                        android.widget.Toast.makeText(actionCtx, "No recent sale to reprint.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                ActionBtn("Returns", Modifier.weight(1f)) { onNav("sales-list") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionBtn("Hold", Modifier.weight(1f)) { vm.holdCurrentTicket() }
                                ActionBtn("Void", Modifier.weight(1f)) { showVoid = true }
                                ActionBtn("Exit", Modifier.weight(1f)) { onNav("home") }
                                Spacer(Modifier.weight(1f))
                            }
                        }
```

- [ ] **Step 4: Host the dialogs** — directly after the `ChargeButton(...)` line (≈ where the old block ended), add:
```kotlin
                        if (showLookup) ItemLookupDialog(vm) { showLookup = false }
                        if (showPrice) PriceDialog(vm) { showPrice = false }
                        if (showVoid) {
                            AlertDialog(
                                onDismissRequest = { showVoid = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        vm.clearCart()
                                        showVoid = false
                                    }) { Text("Void") }
                                },
                                dismissButton = { TextButton(onClick = { showVoid = false }) { Text("Cancel") } },
                                title = { Text("Void this ticket?") },
                                text = { Text("This removes every line from the current ticket.") },
                            )
                        }
```

- [ ] **Step 5: ActionBtn + the two dialog composables** — add near the other private composables (e.g. after `CategoryChip`/`TotalRow`):
```kotlin
@Composable
private fun ActionBtn(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
    }
}

@Composable
private fun ItemLookupDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    var q by remember { mutableStateOf("") }
    val results = vm.products.filter { q.isBlank() || (it.name + " " + it.sku).contains(q, ignoreCase = true) }.take(50)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Item lookup") },
        text = {
            Column(Modifier.widthIn(min = 380.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(c.raised)
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = c.ink),
                        cursorBrush = SolidColor(c.amber),
                        decorationBox = { inner ->
                            if (q.isEmpty()) Text("Search name or SKU…", fontSize = 13.sp, color = c.muted)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    results.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Stock: ${p.stock} · ${p.sku.ifBlank { "no SKU" }}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
                            }
                            Text(rs(p.price), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
                        }
                    }
                    if (results.isEmpty()) Text("No matches.", fontSize = 12.sp, color = c.muted)
                }
            }
        },
    )
}

@Composable
private fun PriceDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Override price") },
        text = {
            Column(Modifier.widthIn(min = 380.dp).heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.workingLines.isEmpty()) {
                    Text("Add items to the ticket first.", fontSize = 12.sp, color = c.muted)
                }
                vm.workingLines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(line.product.name, fontSize = 13.sp, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Row(
                            Modifier.width(120.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.raised)
                                .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Rs", fontSize = 12.sp, color = c.muted)
                            BasicTextField(
                                value = if (line.effectivePrice == 0) "" else line.effectivePrice.toString(),
                                onValueChange = { vm.setLinePrice(line.product.id, it.toIntOrNull() ?: 0) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(c.amber),
                            )
                        }
                    }
                }
            }
        },
    )
}
```
> Symbols already imported in this file: `Box`, `Row`, `Column`, `Spacer`, `clip`, `background`, `border`, `clickable`, `RoundedCornerShape`, `verticalScroll`, `rememberScrollState`, `height`, `width`, `fillMaxWidth`, `padding`, `Alignment`, `FontWeight`, `TextOverflow`, `JetBrainsMono`, `sp`, `dp`, `rs`. Confirm `widthIn`/`heightIn`/`BasicTextField`/`KeyboardOptions`/`KeyboardType`/`TextStyle`/`TextAlign`/`SolidColor`/`AlertDialog`/`TextButton`/`decorationBox` are imported (Step 1).

- [ ] **Step 6: Format, build, full check** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --continue --no-build-cache` → BUILD SUCCESSFUL. Fix any missing import the compiler flags.
- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "feat: POS Actions bar — Lookup, Price, Reprint, Returns, Hold, Void, Exit"
```

---

## Task 3: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL.
- [ ] **Step 2: On-device** — install; POS: **Lookup** shows products with price + stock (read-only); add an item, **Price** → set a custom price → the line total + ticket total update; **Reprint** opens the last receipt (or toasts if none); **Void** clears after confirm; **Exit** → Home; **Returns** → sales list.
- [ ] **Step 3: Push + PR** (stacked on PR #11)
```bash
git push -u origin feature/pos-actions
gh pr create --base main --head feature/pos-actions --title "POS action bar (Pillar B1): lookup, price override, reprint, void, exit" --body "…"
```

---

## Self-review
- **Spec coverage:** price override (T1) ✓; Actions grid Lookup/Price/Reprint/Returns/Hold/Void/Exit (T2) ✓; Item-lookup dialog (T2) ✓; Price-override dialog (T2) ✓; Void confirm (T2) ✓; Reprint→receipt + Exit→home + Returns→sales-list nav (T2) ✓; verify (T3) ✓.
- **Placeholders:** none (T3 PR body `…` filled at push).
- **Type consistency:** `PosLine(..., priceOverride)` + `effectivePrice`; `setLinePrice(productId, priceRupees)`; `ActionBtn(label, modifier, onClick)`; `ItemLookupDialog(vm, onDismiss)`; `PriceDialog(vm, onDismiss)`; screen state `showLookup`/`showPrice`/`showVoid`. Consistent.
- **Risk:** confirm the import set (Step 1); `Customer` is intentionally not in the bar (the CustomerCard already shows + changes it); `vm.clearCart()`/`vm.holdCurrentTicket()`/`vm.lastSale` already exist (used by the old Clear/Hold).
