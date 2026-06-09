# POS Remarks (Pillar B2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the cashier attach a free-text note to a sale via a Remarks button; the note is saved on the sale (new `Sale.note` column, DB v11) and printed on every receipt surface.

**Architecture:** Additive `Sale.note` column + `MIGRATION_10_11`. `SellingViewModel` holds `saleNote`, carries it through `SaleSnapshot` into `persist()`, and resets it per ticket. A Remarks button + dialog on the POS; a "Note:" line on the on-screen, PDF, and HTML receipts.

**Tech Stack:** Kotlin, Room/SQLCipher, Jetpack Compose, JUnit4. Spec: `docs/superpowers/specs/2026-06-09-pos-remarks-design.md`. Branch `feature/pos-remarks` (off main, v10→v11). Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`; `ktlintFormat` in its own invocation.

---

## File structure
**Modify:** `data/entity/Sale.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`, `ui/sale/SellingViewModel.kt`, `ui/sale/PosSaleScreen.kt`, `ui/checkout/PosReceiptScreen.kt`, `ui/checkout/ReceiptOutput.kt`, `test/.../SellingViewModelTest.kt`. **Add (generated):** `app/schemas/com.nexapos.retail.data.PosDatabase/11.json`.

---

## Task 1: Sale.note column + v11 migration

**Files:** Modify `data/entity/Sale.kt`, `data/PosDatabase.kt`, `di/AppContainer.kt`

- [ ] **Step 1: Add the column** — in `Sale.kt`, add the import `import androidx.room.ColumnInfo` and add this field after `customerName` (last field):
```kotlin
    val customerName: String = "Walk-in",
    /** Free-text remark captured at the till (delivery instructions, customer ref). */
    @ColumnInfo(defaultValue = "")
    val note: String = "",
```

- [ ] **Step 2: Bump version + add migration** — in `PosDatabase.kt`, add the comment line after the v10 line and change `version = 10` to `version = 11`:
```kotlin
    // v10: purchases gained a discountCents column (additive, non-destructive MIGRATION_9_10).
    // v11: sales gained a note column (additive, non-destructive MIGRATION_10_11).
    version = 11,
```
and append after `MIGRATION_9_10` (end of file):
```kotlin

/** v10→v11: add sales.note, defaulting existing rows to '' (no remark). */
val MIGRATION_10_11 =
    object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sales ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        }
    }
```

- [ ] **Step 3: Register the migration** — in `AppContainer.kt`, change:
```kotlin
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
```
to:
```kotlin
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
```
and add the import `import com.nexapos.retail.data.MIGRATION_10_11` alongside the other `MIGRATION_*` imports.

- [ ] **Step 4: Build to generate the schema** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL; confirm `app/schemas/com.nexapos.retail.data.PosDatabase/11.json` was created and that its `sales` table lists `note` with `"defaultValue": "''"`.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/data/entity/Sale.kt app/src/main/java/com/nexapos/retail/data/PosDatabase.kt app/src/main/java/com/nexapos/retail/di/AppContainer.kt app/schemas/com.nexapos.retail.data.PosDatabase/11.json
git commit -m "feat: Sale.note column + MIGRATION_10_11 (v11, additive)"
```

---

## Task 2: ViewModel — saleNote through to persist (TDD)

**Files:** Modify `ui/sale/SellingViewModel.kt`, `test/.../SellingViewModelTest.kt`

- [ ] **Step 1: Write the failing test** — add to `SellingViewModelTest`:
```kotlin
    @Test
    fun `sale note is persisted on the sale`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.saleNote = "Deliver tomorrow AM"
            model.beginCheckout()
            model.complete()
            val (sale, _) = sales.recorded.first()
            assertEquals("Deliver tomorrow AM", sale.note)
        }
```

- [ ] **Step 2: Run to verify it fails** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*SellingViewModelTest*" --rerun-tasks --no-build-cache` → FAIL (`saleNote` unresolved).

- [ ] **Step 3: Add the state** — in `SellingViewModel`, add after `var discountPercent by mutableStateOf(0)`:
```kotlin

    /** Free-text remark for the current ticket; printed on the receipt + saved on the sale. */
    var saleNote by mutableStateOf("")
```

- [ ] **Step 4: SaleSnapshot.note** — add to the `SaleSnapshot` data class after `creditDue`:
```kotlin
    val creditDue: Int = 0,
    /** Free-text remark captured at the till. */
    val note: String = "",
```

- [ ] **Step 5: Set note in the snapshot** — in `complete()`, in the `SaleSnapshot(...)` construction, change `creditDue = creditDue,` to:
```kotlin
                creditDue = creditDue,
                note = saleNote.trim(),
```

- [ ] **Step 6: Persist the note** — in `persist()`, in the `Sale(...)` construction, change `customerName = customer?.name ?: snapshot.customerName,` to:
```kotlin
                customerName = customer?.name ?: snapshot.customerName,
                note = snapshot.note,
```

- [ ] **Step 7: Reset per ticket** — in `startNewTicket()` add `saleNote = ""` (after `selectedCustomer = null`); in `clearCart()` change the body to:
```kotlin
    fun clearCart() {
        workingLines.clear()
        saleNote = ""
    }
```

- [ ] **Step 8: Run to verify it passes** — same command → PASS (whole suite).
- [ ] **Step 9: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/SellingViewModel.kt app/src/test/java/com/nexapos/retail/ui/sale/SellingViewModelTest.kt
git commit -m "feat: carry saleNote through SaleSnapshot to the persisted sale"
```

---

## Task 3: Remarks button + dialog (PosSaleScreen)

**Files:** Modify `ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Dialog state** — with the other dialog state (`var showVoid …`), add:
```kotlin
    var showRemarks by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Remarks button** — in the action grid's second row, replace:
```kotlin
                                ActionBtn("Exit", Modifier.weight(1f)) { onNav("home") }
                                Spacer(Modifier.weight(1f))
```
with:
```kotlin
                                ActionBtn("Exit", Modifier.weight(1f)) { onNav("home") }
                                ActionBtn(if (vm.saleNote.isBlank()) "Remarks" else "Remarks •", Modifier.weight(1f)) { showRemarks = true }
```

- [ ] **Step 3: Host the dialog** — next to the other dialog hosts (after `if (showPrice) PriceDialog(vm) { showPrice = false }`), add:
```kotlin
                        if (showRemarks) RemarksDialog(vm) { showRemarks = false }
```

- [ ] **Step 4: The dialog composable** — add after `PriceDialog` (the private composables block):
```kotlin
@Composable
private fun RemarksDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = { vm.saleNote = "" }) { Text("Clear") } },
        title = { Text("Remarks") },
        text = {
            Column(Modifier.widthIn(min = 380.dp)) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(10.dp))
                        .background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(12.dp),
                ) {
                    BasicTextField(
                        value = vm.saleNote,
                        onValueChange = { vm.saleNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = c.ink),
                        cursorBrush = SolidColor(c.amber),
                        decorationBox = { inner ->
                            if (vm.saleNote.isEmpty()) Text("Delivery instructions, customer ref…", fontSize = 13.sp, color = c.muted)
                            inner()
                        },
                    )
                }
            }
        },
    )
}
```
> All symbols here (`AlertDialog`, `TextButton`, `BasicTextField`, `TextStyle`, `SolidColor`, `widthIn`, `heightIn`, `RoundedCornerShape`, `Box`, `Column`) were already imported for the B1 dialogs — no new imports.

- [ ] **Step 5: Build** — `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "feat: POS Remarks button + dialog (bound to saleNote)"
```

---

## Task 4: Print the note on receipts

**Files:** Modify `ui/checkout/PosReceiptScreen.kt`, `ui/checkout/ReceiptOutput.kt`

- [ ] **Step 1: On-screen receipt** — in `PosReceiptScreen.kt`, replace:
```kotlin
            DashedLine()
            Text(
                com.nexapos.retail.data.profile.ReceiptSettings.footerNote(context),
```
with:
```kotlin
            if (sale.note.isNotBlank()) {
                Text("Note: ${sale.note}", fontFamily = JetBrainsMono, fontSize = 10.sp, color = receiptInk, modifier = Modifier.padding(top = 4.dp))
            }
            DashedLine()
            Text(
                com.nexapos.retail.data.profile.ReceiptSettings.footerNote(context),
```

- [ ] **Step 2: PDF/print receipt** — in `ReceiptOutput.kt`, replace:
```kotlin
        } else {
            row("Change", p.label, money(maxOf(0, sale.change)), p.valueR)
        }
        dash()
        ReceiptSettings.footerNote(context).takeIf { it.isNotBlank() }?.let { centre(it, p.foot) }
```
with:
```kotlin
        } else {
            row("Change", p.label, money(maxOf(0, sale.change)), p.valueR)
        }
        if (sale.note.isNotBlank()) left("Note: ${sale.note}", p.item)
        dash()
        ReceiptSettings.footerNote(context).takeIf { it.isNotBlank() }?.let { centre(it, p.foot) }
```

- [ ] **Step 3: HTML receipt** — in `ReceiptOutput.kt`, replace:
```kotlin
                $tailRow
              </table>
              <hr>
              <div class="ft">$footer</div>
```
with:
```kotlin
                $tailRow
              </table>
              ${if (sale.note.isNotBlank()) "<div class=\"ft\" style=\"text-align:left;margin-top:6px\">Note: ${esc(sale.note)}</div>" else ""}
              <hr>
              <div class="ft">$footer</div>
```

- [ ] **Step 4: Format, build, full check** — `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --continue --no-build-cache` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/checkout/PosReceiptScreen.kt app/src/main/java/com/nexapos/retail/ui/checkout/ReceiptOutput.kt
git commit -m "feat: print the sale note on the on-screen, PDF, and HTML receipts"
```

---

## Task 5: Verification

- [ ] **Step 1: Full checks** — `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL; the note test green; `11.json` committed.
- [ ] **Step 2: Migration safety** — confirm `Sale.note`'s `@ColumnInfo(defaultValue = "")` matches `MIGRATION_10_11`'s `DEFAULT ''` and that `11.json`'s `sales.note` shows `"notNull": true, "defaultValue": "''"` (so a v10→v11 upgrade is additive, not a destructive rebuild).
- [ ] **Step 3: On-device** — install; POS → **Remarks** → type a note (button shows "Remarks •") → Charge → complete: the receipt shows the "Note:" line; reopen the sale / share PDF shows it too. New ticket clears the note.
- [ ] **Step 4: Push + PR**
```bash
git push -u origin feature/pos-remarks
gh pr create --base main --head feature/pos-remarks --title "POS Remarks (Pillar B2): sale note + receipt print" --body "…"
```

---

## Self-review
- **Spec coverage:** Sale.note + migration + 11.json (T1) ✓; saleNote state + SaleSnapshot.note + persist + reset + unit test (T2) ✓; Remarks button + dialog (T3) ✓; note on on-screen/PDF/HTML receipts (T4) ✓; migration safety + verify (T5) ✓.
- **Placeholders:** none (T5 PR body `…` filled at push).
- **Type consistency:** `Sale.note`; `MIGRATION_10_11`; `saleNote`; `SaleSnapshot.note`; `RemarksDialog(vm, onDismiss)`; `showRemarks`. The snapshot sets `note = saleNote.trim()`, persist reads `snapshot.note`, receipts read `sale.note`. Consistent.
- **Risk:** the note survives `beginCheckout()` (only `startNewTicket`/`clearCart` reset it) so it persists with the sale — covered by the T2 test. The Remarks button replaces the row-2 `Spacer`, so the action grid stays 4-wide.
