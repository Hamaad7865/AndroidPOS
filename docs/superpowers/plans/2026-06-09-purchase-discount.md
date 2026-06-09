# Purchase Supplier Discount Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record a supplier discount (% or Rs) on a purchase order so `Total = Subtotal − Discount`, persisted and shown on the PO detail.

**Architecture:** `Purchase` gains a flat `discountCents` (non-destructive v9→v10 migration). `recordPurchaseFromDraft` takes `discountRupees`, clamps it, and stores the net total. The New-PO form gets a `% / Rs` discount control (reusing the sale's `percentToFlat`).

**Tech Stack:** Kotlin, Jetpack Compose, Room (SQLCipher), JUnit4. Spec: `docs/superpowers/specs/2026-06-09-purchase-discount-design.md`. Branch `feature/purchase-discount` (off main, DB v10). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation; add `--rerun-tasks --no-build-cache` to tests when stale.

---

## File structure
**Create:** `MIGRATION_9_10` (in `PosDatabase.kt`).
**Modify:** `data/entity/Purchase.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`, `ui/purchase/PurchaseRecorder.kt`, `ui/purchase/PurchasesViewModel.kt`, `ui/purchase/PurchaseForms.kt`, `test/.../PurchaseRecorderTest.kt`.

---

## Task 1: Purchase.discountCents + migration v9→v10

**Files:** Modify `data/entity/Purchase.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`

- [ ] **Step 1: Add the column** — in `Purchase.kt`, add `import androidx.room.ColumnInfo`, and after `val totalCents: Long,` add:
```kotlin
    /** Flat Rs supplier discount on the order (minor units); totalCents is already net of it.
     *  Declared with a SQL default so the v9→v10 migration matches Room's schema (no wipe). */
    @ColumnInfo(defaultValue = "0")
    val discountCents: Long = 0,
```

- [ ] **Step 2: Migration + version bump** — in `PosDatabase.kt`, change `version = 9,` to `version = 10,`, add the comment `// v10: purchases gained a discountCents column (additive, non-destructive MIGRATION_9_10).` above it, and add below `MIGRATION_8_9`:
```kotlin

/** v9→v10: add purchases.discountCents, defaulting existing rows to 0 (no supplier discount). */
val MIGRATION_9_10 =
    object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE purchases ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0")
        }
    }
```

- [ ] **Step 3: Register it** — in `AppContainer.kt`, add `import com.nexapos.retail.data.MIGRATION_9_10`, and change `.addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)` to `.addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)`.

- [ ] **Step 4: Build + check schema** — `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug` → BUILD SUCCESSFUL. Confirm: `find app -name "10.json" -path "*PosDatabase*" | head -1 | xargs grep -o 'discountCents` INTEGER NOT NULL DEFAULT 0'` prints a match.

- [ ] **Step 5: Commit** (include `app/schemas/.../10.json`)
```bash
git add app/src/main/java/com/nexapos/retail/data/entity/Purchase.kt app/src/main/java/com/nexapos/retail/data/PosDatabase.kt app/src/main/java/com/nexapos/retail/di/AppContainer.kt app/schemas/com.nexapos.retail.data.PosDatabase/10.json
git commit -m "feat: purchases.discountCents column + non-destructive migration v9->v10"
```

---

## Task 2: recordPurchaseFromDraft discount (TDD)

**Files:** Modify `ui/purchase/PurchaseRecorder.kt`, `test/.../PurchaseRecorderTest.kt`

- [ ] **Step 1: Write the failing tests** — add to `PurchaseRecorderTest` (inside the class):
```kotlin
    @Test
    fun `applies supplier discount to the order total`() =
        runTest {
            val fakes = RecorderFakes()
            recordPurchaseFromDraft(
                purchasesRepository = fakes.purchases,
                catalogRepository = fakes.catalog,
                partiesRepository = fakes.parties,
                supplierName = "ACME",
                paymentMethod = "cash",
                items = listOf(PurchaseDraftItem("Pipe", quantity = 10, unitCostRupees = 100)), // subtotal 1000
                discountRupees = 150,
            )
            val recorded = fakes.purchases.recorded.single().first
            assertEquals(150 * 100L, recorded.discountCents)
            assertEquals((1000 - 150) * 100L, recorded.totalCents)
        }

    @Test
    fun `discount is clamped to the subtotal`() =
        runTest {
            val fakes = RecorderFakes()
            recordPurchaseFromDraft(
                purchasesRepository = fakes.purchases,
                catalogRepository = fakes.catalog,
                partiesRepository = fakes.parties,
                supplierName = "ACME",
                paymentMethod = "cash",
                items = listOf(PurchaseDraftItem("Pipe", quantity = 1, unitCostRupees = 100)), // subtotal 100
                discountRupees = 500,
            )
            val recorded = fakes.purchases.recorded.single().first
            assertEquals(100 * 100L, recorded.discountCents) // clamped to subtotal
            assertEquals(0L, recorded.totalCents)
        }
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*PurchaseRecorderTest*" --rerun-tasks --no-build-cache` → FAIL (no `discountRupees` param / no `discountCents`).

- [ ] **Step 3: Implement** — in `PurchaseRecorder.kt`:
  - add the trailing parameter to `recordPurchaseFromDraft` (after `notes: String = "",`):
```kotlin
    discountRupees: Int = 0,
```
  - replace the `Purchase(...)` construction's `totalCents = lines.sumOf { it.lineTotalCents },` line with:
```kotlin
            totalCents = run { val s = lines.sumOf { it.lineTotalCents }; s - (discountRupees * CENTS_PER_RUPEE).coerceIn(0, s) },
            discountCents = (discountRupees * CENTS_PER_RUPEE).coerceIn(0, lines.sumOf { it.lineTotalCents }),
```
  (Both clamp to the gross subtotal; `totalCents` = subtotal − clamped discount.)

- [ ] **Step 4: Run to verify it passes** — same command → PASS (all recorder tests).
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseRecorder.kt app/src/test/java/com/nexapos/retail/ui/purchase/PurchaseRecorderTest.kt
git commit -m "feat: supplier discount in recordPurchaseFromDraft (net total, clamp, stored)"
```

---

## Task 3: PurchasesViewModel passes the discount

**Files:** Modify `ui/purchase/PurchasesViewModel.kt`

- [ ] **Step 1: Add the param + forward it** — in `recordPurchase`, add a parameter after `notes: String = "",`:
```kotlin
        discountRupees: Int = 0,
```
and add `discountRupees = discountRupees,` as the final argument to the `recordPurchaseFromDraft(...)` call (after `notes,` — switch that call to named args if needed; it currently uses positional args, so append `discountRupees` last):
```kotlin
            recordPurchaseFromDraft(
                purchasesRepository,
                catalogRepository,
                partiesRepository,
                supplierName,
                paymentMethod,
                items,
                status,
                expectedDelivery,
                notes,
                discountRupees,
            )
```

- [ ] **Step 2: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/PurchasesViewModel.kt
git commit -m "feat: PurchasesViewModel.recordPurchase forwards the supplier discount"
```

---

## Task 4: New-PO discount control + detail line

**Files:** Modify `ui/purchase/PurchaseForms.kt`

- [ ] **Step 1: Imports** — ensure these are imported (add any missing): `androidx.compose.foundation.text.BasicTextField`, `androidx.compose.foundation.text.KeyboardOptions`, `androidx.compose.ui.text.TextStyle`, `androidx.compose.ui.text.input.KeyboardType`, `androidx.compose.ui.text.style.TextAlign`, `androidx.compose.ui.graphics.SolidColor`, `androidx.compose.foundation.border`, `androidx.compose.foundation.background`, `androidx.compose.ui.draw.clip`, `androidx.compose.foundation.shape.RoundedCornerShape`, and `com.nexapos.retail.ui.sale.percentToFlat`. (Read the existing import block first; most are already present.)

- [ ] **Step 2: Discount state + net total** — right after `val subtotal = items.sumOf { it.quantity * it.unitCostRupees }` (≈ line 112) add:
```kotlin
    var discountIsPercent by remember { mutableStateOf(false) }
    var discountValue by remember { mutableStateOf(0) }
    val discountFlat = (if (discountIsPercent) percentToFlat(subtotal, discountValue) else discountValue).coerceIn(0, subtotal)
    val total = subtotal - discountFlat
```

- [ ] **Step 3: Confirm passes the discount** — in `confirm()`, add `discountRupees = discountFlat,` to the `vm.recordPurchase(...)` call (after `notes = notes,`).

- [ ] **Step 4: The control + net TOTAL** — replace the summary block from `SumRow("Subtotal", rsStr(subtotal), mono = true)` (≈ line 331) through the TOTAL `Text(rsStr(subtotal), …)` with:
```kotlin
                SumRow("Subtotal", rsStr(subtotal), mono = true)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Discount", fontSize = 12.sp, color = c.muted)
                    Row(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, c.hairline, RoundedCornerShape(7.dp))) {
                        DiscMode("%", discountIsPercent) { discountIsPercent = true }
                        DiscMode("Rs", !discountIsPercent) { discountIsPercent = false }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.raised2)
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!discountIsPercent) Text("Rs", fontSize = 13.sp, color = c.muted)
                    BasicTextField(
                        value = if (discountValue == 0) "" else discountValue.toString(),
                        onValueChange = { discountValue = it.toIntOrNull() ?: 0 },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(c.amber),
                    )
                    if (discountIsPercent) Text("%", fontSize = 13.sp, color = c.muted)
                }
                if (discountFlat > 0) {
                    Spacer(Modifier.height(6.dp))
                    SumRow("Discount", "− ${rsStr(discountFlat)}", mono = true)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontSize = 13.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
                    Text(
                        rsStr(total),
                        fontFamily = JetBrainsMono,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.ink,
                    )
                }
```
(This replaces the existing `SumRow("Subtotal"…)` + the divider + the old TOTAL row that showed `rsStr(subtotal)`.)

- [ ] **Step 5: The `DiscMode` chip** — add a private composable near the other helpers in this file:
```kotlin
@Composable
private fun DiscMode(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) c.ink else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) c.surface else c.ink)
    }
}
```
(`clickable` is already used in this file.)

- [ ] **Step 6: Detail card discount line** — in `DetailTotalsCard`, after `SumRow("Subtotal", rsStr(subtotal), mono = true)` (≈ line 819) add:
```kotlin
        if (purchase.discountCents > 0) {
            Spacer(Modifier.height(6.dp))
            SumRow("Discount", "− ${rsStr((purchase.discountCents / CENTS_PER_RUPEE).toInt())}", mono = true)
        }
```

- [ ] **Step 7: Format, build, lint** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck` → BUILD SUCCESSFUL. Fix any missing import the compiler flags.
- [ ] **Step 8: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseForms.kt
git commit -m "feat: New PO supplier discount control (% / Rs) + detail discount line"
```

---

## Task 5: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL; `PurchaseRecorderTest` green.
- [ ] **Step 2: Migration test (v9→v10 preserves data)** — `adb install -r app/build/outputs/apk/debug/app-debug.apk` over the v9 emulator DB; relaunch, sign in → existing purchases/products still present.
- [ ] **Step 3: On-device** — Purchase → New PO → add an item (e.g. qty 10 × Rs 100 = Rs 1,000), set Discount = 10% → Total shows Rs 900; Confirm; open the PO detail → it shows Subtotal Rs 1,000, Discount − Rs 100, Total Rs 900.
- [ ] **Step 4: Push + PR**
```bash
git push -u origin feature/purchase-discount
gh pr create --base main --head feature/purchase-discount --title "Purchase: supplier discount" --body "…"
```

---

## Self-review
- **Spec coverage:** discountCents + migration (T1) ✓; recorder discount + clamp + net total + tests (T2) ✓; VM forwards discount (T3) ✓; form control %/Rs + net total + confirm + detail line (T4) ✓; migration + on-device (T5) ✓.
- **Placeholders:** none (T5 PR body `…` filled at push).
- **Type consistency:** `Purchase.discountCents: Long`; `recordPurchaseFromDraft(… discountRupees: Int = 0)`; `PurchasesViewModel.recordPurchase(… discountRupees: Int = 0)`; form `discountIsPercent`/`discountValue`/`discountFlat`/`total`; `percentToFlat(subtotal, pct)`; `DiscMode(label, selected, onClick)`. Consistent.
- **Risk:** T4 — confirm the summary block's exact lines before replacing (read 326-344 first); the receipt-scan path keeps the default `discountRupees = 0` (no edit).
