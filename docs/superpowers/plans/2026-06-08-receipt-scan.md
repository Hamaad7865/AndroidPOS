# Receipt Scan → Register Purchase — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the owner photograph a supplier's hardcopy receipt, review an auto-extracted draft, and register it as a Purchase (supplier + items + cost + stock) with the catalog kept in sync — fully offline.

**Architecture:** A single `ReceiptScanScreen`/`ReceiptScanViewModel` drives capture → on-device ML Kit OCR → a pure-Kotlin `ReceiptParser` → an editable review → registration. Registration reuses a newly-extracted, unit-tested `recordPurchaseFromDraft` (also adopted by the existing purchase form, for DRY). The receipt photo is stored via the existing `ImageStore` keyed by PO code — no DB schema change.

**Tech Stack:** Kotlin, Jetpack Compose, ML Kit bundled text-recognition, Room+SQLCipher, manual DI (`AppViewModelProvider`/`AppContainer`). Build: `./gradlew assembleDebug`. Tests: `./gradlew testDebugUnitTest detekt ktlintCheck`.

**Spec:** `docs/superpowers/specs/2026-06-08-receipt-scan-design.md`

---

## File Structure

**Create:**
- `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptModels.kt` — pure data types (`OcrLine`, `ReceiptDraftLine`, `ParsedReceipt`).
- `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParser.kt` — pure parser (`List<OcrLine>` → `ParsedReceipt`). No Android deps.
- `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptOcr.kt` — ML Kit wrapper (`Uri` → `List<OcrLine>`). Android.
- `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanViewModel.kt` — capture/ocr/draft state + register.
- `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanScreen.kt` — the one-screen flow.
- `app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseRecorder.kt` — shared `recordPurchaseFromDraft(...)`.
- `app/src/test/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParserTest.kt` — parser unit tests.
- `app/src/test/java/com/nexapos/retail/ui/purchase/PurchaseRecorderTest.kt` — recorder unit test.
- `app/src/main/res/xml/file_paths.xml` — FileProvider paths for camera capture.

**Modify:**
- `gradle/libs.versions.toml` — add ML Kit text-recognition.
- `app/build.gradle.kts` — wire the dependency.
- `app/src/main/AndroidManifest.xml` — add the FileProvider + camera feature.
- `app/src/main/java/com/nexapos/retail/ui/purchase/PurchasesViewModel.kt` — delegate `recordPurchase` to the shared recorder.
- `app/src/main/java/com/nexapos/retail/ui/AppViewModelProvider.kt` — register `ReceiptScanViewModel`.
- `app/src/main/java/com/nexapos/retail/ui/PosApp.kt` — add the `receipt-scan` route + pass `onScanReceipt` to the Purchase screen.
- `app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseScreen.kt` — add the "Scan receipt" button.

---

## Task 1: Add ML Kit text-recognition dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version + library to the catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:
```toml
mlkitTextRecognition = "16.0.1"
```
Under `[libraries]` add:
```toml
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognition" }
```

- [ ] **Step 2: Wire it into the app module**

In `app/build.gradle.kts`, in the `dependencies { }` block next to the other `implementation(...)` lines (near `play.services.code.scanner`), add:
```kotlin
implementation(libs.mlkit.text.recognition)
```

- [ ] **Step 3: Verify it resolves**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath` (or `./gradlew assembleDebug`)
Expected: BUILD SUCCESSFUL; `com.google.mlkit:text-recognition:16.0.1` appears in the tree.

- [ ] **Step 4: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add ML Kit bundled text-recognition for receipt OCR"
```

---

## Task 2: Pure receipt data types

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptModels.kt`

- [ ] **Step 1: Create the file with pure data types**
```kotlin
package com.nexapos.retail.ui.purchase.receipt

/**
 * One line of recognised text with its bounding box (pixels). Kept free of
 * Android types so [ReceiptParser] can be unit-tested without a device.
 */
data class OcrLine(
    val text: String,
    val top: Int,
    val left: Int,
    val right: Int,
)

/** One editable draft line on the review screen (whole-rupee unit cost). */
data class ReceiptDraftLine(
    val name: String,
    val quantity: Int,
    val unitCostRupees: Int,
)

/** Best-effort result of parsing a receipt's OCR lines. */
data class ParsedReceipt(
    val supplierGuess: String,
    val lines: List<ReceiptDraftLine>,
    val warnings: List<String>,
)
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptModels.kt
git commit -m "feat: add pure receipt draft data types"
```

---

## Task 3: ReceiptParser (TDD)

**Files:**
- Create: `app/src/test/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParserTest.kt`
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParser.kt`

- [ ] **Step 1: Write failing tests**

Create `ReceiptParserTest.kt`:
```kotlin
package com.nexapos.retail.ui.purchase.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    // Helper: build OcrLines top-to-bottom; geometry isn't needed for these cases.
    private fun lines(vararg text: String): List<OcrLine> =
        text.mapIndexed { i, t -> OcrLine(t, top = i * 30, left = 0, right = 400) }

    @Test
    fun `extracts qty name and unit cost from a printed line`() {
        val parsed = ReceiptParser.parse(lines("2 Hammer Claw 16oz   250.00"))
        assertEquals(1, parsed.lines.size)
        assertEquals(ReceiptDraftLine("Hammer Claw 16oz", 2, 250), parsed.lines.first())
    }

    @Test
    fun `defaults quantity to 1 when absent`() {
        val parsed = ReceiptParser.parse(lines("Cement Bag 50kg   Rs 520"))
        assertEquals(ReceiptDraftLine("Cement Bag 50kg", 1, 520), parsed.lines.first())
    }

    @Test
    fun `excludes total vat and subtotal lines`() {
        val parsed = ReceiptParser.parse(
            lines(
                "Nails 1kg 45",
                "Subtotal 45",
                "VAT 15% 6.75",
                "TOTAL 51.75",
                "Balance Due 51.75",
            ),
        )
        assertEquals(listOf("Nails 1kg"), parsed.lines.map { it.name })
    }

    @Test
    fun `guesses supplier from the top non-amount line`() {
        val parsed = ReceiptParser.parse(lines("ACME HARDWARE LTD", "Tel 5712 3456", "Bolt M8 x10  120"))
        assertEquals("ACME HARDWARE LTD", parsed.supplierGuess)
    }

    @Test
    fun `handles messy money formats`() {
        val parsed = ReceiptParser.parse(lines("Paint White 5L  Rs 1,200.50"))
        assertEquals(1200, parsed.lines.first().unitCostRupees)
    }

    @Test
    fun `empty input yields empty draft and a warning`() {
        val parsed = ReceiptParser.parse(emptyList())
        assertTrue(parsed.lines.isEmpty())
        assertTrue(parsed.warnings.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nexapos.retail.ui.purchase.receipt.ReceiptParserTest"`
Expected: FAIL — `ReceiptParser` unresolved.

- [ ] **Step 3: Implement the parser**

Create `ReceiptParser.kt`:
```kotlin
package com.nexapos.retail.ui.purchase.receipt

/**
 * Best-effort extraction of supplier + line items from a receipt's OCR lines.
 * Intentionally forgiving: the editable review screen is the safety net, so this
 * aims for "good enough to fix fast", not perfection. Pure — unit-tested.
 */
object ReceiptParser {
    // A trailing money amount: optional "Rs", digits with , or . groupings.
    private val AMOUNT = Regex("""(?:rs\.?\s*)?(\d[\d.,]*\d|\d)\s*$""", RegexOption.IGNORE_CASE)
    private val LEADING_QTY = Regex("""^\s*(\d{1,3})\s*(?:x|\*)?\s+""", RegexOption.IGNORE_CASE)
    private val NON_ITEM = Regex(
        """\b(sub-?total|total|vat|tva|tax|balance|due|change|tendered|cash|amount|discount|rounding)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(lines: List<OcrLine>): ParsedReceipt {
        if (lines.isEmpty()) {
            return ParsedReceipt("", emptyList(), listOf("Nothing was read from the image."))
        }
        val ordered = lines.sortedBy { it.top }
        val supplier = ordered.firstOrNull { !looksLikeAmountOrNoise(it.text) }?.text?.trim().orEmpty()

        val items = mutableListOf<ReceiptDraftLine>()
        var skipped = 0
        for (line in ordered) {
            val raw = line.text.trim()
            if (raw.isBlank()) continue
            if (NON_ITEM.containsMatchIn(raw)) continue
            val amount = AMOUNT.find(raw) ?: continue
            val unitCost = parseMoney(amount.groupValues[1])
            if (unitCost <= 0) {
                skipped++
                continue
            }
            var name = raw.removeRange(amount.range).trim().trimEnd('-', '.', ' ')
            var qty = 1
            LEADING_QTY.find(name)?.let { m ->
                qty = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                name = name.removeRange(m.range).trim()
            }
            if (name.isBlank() || name.length < 2) {
                skipped++
                continue
            }
            items += ReceiptDraftLine(name = name, quantity = qty, unitCostRupees = unitCost)
        }

        val warnings = buildList {
            if (items.isEmpty()) add("No item lines were recognised — add them manually.")
            if (skipped > 0) add("$skipped line(s) couldn't be read clearly — please check.")
        }
        return ParsedReceipt(supplierGuess = supplier, lines = items, warnings = warnings)
    }

    private fun looksLikeAmountOrNoise(text: String): Boolean {
        val t = text.trim()
        if (t.length < 3) return true
        val digits = t.count { it.isDigit() }
        return digits > t.length / 2 // mostly numbers (phone, amounts, dates)
    }

    /** Whole-rupee amount from a cell like "1,200.50" → 1200 (drops cents). */
    private fun parseMoney(cell: String): Int =
        cell.replace(",", "").filter { it.isDigit() || it == '.' }
            .toDoubleOrNull()?.toInt() ?: 0
}
```

- [ ] **Step 4: Run tests, verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nexapos.retail.ui.purchase.receipt.ReceiptParserTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParser.kt app/src/test/java/com/nexapos/retail/ui/purchase/receipt/ReceiptParserTest.kt
git commit -m "feat: add unit-tested receipt OCR-line parser"
```

---

## Task 4: Extract shared `recordPurchaseFromDraft` (TDD)

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseRecorder.kt`
- Create: `app/src/test/java/com/nexapos/retail/ui/purchase/PurchaseRecorderTest.kt`
- Modify: `app/src/main/java/com/nexapos/retail/ui/purchase/PurchasesViewModel.kt`

- [ ] **Step 1: Write the failing recorder test**

Create `PurchaseRecorderTest.kt` (uses the existing test fakes — confirm `FakeCatalogRepository` exists under `app/src/test/java/com/nexapos/retail/fake/`; create lightweight fakes for `PurchasesRepository`/`PartiesRepository` inline if not present):
```kotlin
package com.nexapos.retail.ui.purchase

import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PurchaseRecorderTest {
    @Test
    fun `records purchase, creates unknown product, links and totals`() = runTest {
        val fakes = RecorderFakes(existing = listOf(Product(id = 7, name = "Hammer", priceCents = 25000)))
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "ACME",
            paymentMethod = "cash",
            items = listOf(
                PurchaseDraftItem("Hammer", quantity = 2, unitCostRupees = 250),
                PurchaseDraftItem("Brand New Bolt", quantity = 10, unitCostRupees = 5),
            ),
        )
        val recorded = fakes.purchases.recorded.single()
        assertEquals("ACME", recorded.first.supplierName)
        assertEquals(2 + 10, recorded.first.itemCount)
        assertEquals((2 * 250 + 10 * 5) * 100L, recorded.first.totalCents)
        // Known product linked by id; new product created and linked.
        assertEquals(7L, recorded.second.first { it.nameSnapshot == "Hammer" }.productId)
        assertNotNull(recorded.second.first { it.nameSnapshot == "Brand New Bolt" }.productId)
    }
}
```
*(If reusable fakes for purchases/parties don't already exist in the test tree, add a small `RecorderFakes` helper in this test file that implements the three repository interfaces with in-memory lists. Match the interfaces in `domain/repository/`.)*

- [ ] **Step 2: Run it, verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.nexapos.retail.ui.purchase.PurchaseRecorderTest"`
Expected: FAIL — `recordPurchaseFromDraft` unresolved.

- [ ] **Step 3: Implement the shared recorder**

Create `PurchaseRecorder.kt` by lifting the body of `PurchasesViewModel.recordPurchase` (lines 111–165) into a standalone suspend function that fetches the catalog/suppliers/count from the repos (self-contained, no VM cache):
```kotlin
package com.nexapos.retail.ui.purchase

import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.first

private const val CENTS_PER_RUPEE = 100L
private const val NEXT_CODE_BASE = 1043

/**
 * Records a purchase from draft lines and (when received) raises stock — creating
 * the supplier and any brand-new products so each line links to a real productId.
 * Shared by the purchase form and the receipt scanner. Returns the purchase id.
 */
suspend fun recordPurchaseFromDraft(
    purchasesRepository: PurchasesRepository,
    catalogRepository: CatalogRepository,
    partiesRepository: PartiesRepository,
    supplierName: String,
    paymentMethod: String,
    items: List<PurchaseDraftItem>,
    status: String = "received",
    expectedDelivery: String = "",
    notes: String = "",
): Long {
    val trimmedName = supplierName.trim()
    if (trimmedName.isBlank() || items.isEmpty()) return -1L

    val suppliers = partiesRepository.observeSuppliers().first()
    if (suppliers.none { it.name.equals(trimmedName, ignoreCase = true) }) {
        partiesRepository.upsert(
            Party(
                name = trimmedName,
                type = Party.TYPE_SUPPLIER,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    val products = catalogRepository.observeAllProducts().first()
    val nameToId = products.associate { it.name.lowercase() to it.id }.toMutableMap()
    items.forEach { draft ->
        val key = draft.name.trim().lowercase()
        if (key.isNotEmpty() && nameToId[key] == null) {
            val unitCostCents = draft.unitCostRupees * CENTS_PER_RUPEE
            val newId = catalogRepository.upsert(
                Product(
                    name = draft.name.trim(),
                    priceCents = unitCostCents,
                    costCents = unitCostCents,
                    stockQty = 0,
                    isActive = true,
                ),
            )
            nameToId[key] = newId
        }
    }

    val lines = items.map { draft ->
        PurchaseItem(
            purchaseId = 0,
            productId = nameToId[draft.name.trim().lowercase()],
            nameSnapshot = draft.name.trim(),
            unitCostCents = draft.unitCostRupees * CENTS_PER_RUPEE,
            quantity = draft.quantity,
            lineTotalCents = draft.quantity * draft.unitCostRupees * CENTS_PER_RUPEE,
        )
    }
    val orderCount = purchasesRepository.observeRecent().first().size
    val purchase = Purchase(
        code = "PO-%04d".format(NEXT_CODE_BASE + orderCount),
        supplierName = trimmedName,
        createdAt = System.currentTimeMillis(),
        itemCount = items.sumOf { it.quantity },
        totalCents = lines.sumOf { it.lineTotalCents },
        paymentMethod = paymentMethod,
        status = status,
        expectedDelivery = expectedDelivery.trim(),
        notes = notes.trim(),
    )
    return purchasesRepository.recordPurchase(purchase, lines)
}
```

- [ ] **Step 4: Route `PurchasesViewModel.recordPurchase` through it**

In `PurchasesViewModel.kt`, replace the body of `recordPurchase` (the `viewModelScope.launch { ... }` block, lines 112–166) with a delegating call (keep the same public signature):
```kotlin
fun recordPurchase(
    supplierName: String,
    paymentMethod: String,
    items: List<PurchaseDraftItem>,
    status: String = "received",
    expectedDelivery: String = "",
    notes: String = "",
) {
    if (supplierName.isBlank() || items.isEmpty()) return
    viewModelScope.launch {
        recordPurchaseFromDraft(
            purchasesRepository, catalogRepository, partiesRepository,
            supplierName, paymentMethod, items, status, expectedDelivery, notes,
        )
    }
}
```
Delete the now-unused private `NEXT_CODE_BASE`/`CENTS_PER_RUPEE` only if nothing else in the file uses them (the `monthTotal`/`suggestedCost` getters use `CENTS_PER_RUPEE` — keep it). Leave `addSupplier`, `findSupplier`, `suggestedCost` intact.

- [ ] **Step 5: Run recorder + existing purchase tests**

Run: `./gradlew testDebugUnitTest --tests "com.nexapos.retail.ui.purchase.*"`
Expected: PASS.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseRecorder.kt app/src/test/java/com/nexapos/retail/ui/purchase/PurchaseRecorderTest.kt app/src/main/java/com/nexapos/retail/ui/purchase/PurchasesViewModel.kt
git commit -m "refactor: extract shared recordPurchaseFromDraft, reuse in PurchasesViewModel"
```

---

## Task 5: ReceiptOcr (ML Kit wrapper)

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptOcr.kt`

*(Not unit-tested — ML Kit needs the on-device model; verified manually in Task 9.)*

- [ ] **Step 1: Implement the wrapper**
```kotlin
package com.nexapos.retail.ui.purchase.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** On-device OCR (bundled ML Kit Latin model). Returns recognised lines + boxes. */
object ReceiptOcr {
    suspend fun recognise(
        context: Context,
        imageUri: Uri,
    ): List<OcrLine> {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result =
            suspendCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        return result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val box = line.boundingBox
                OcrLine(
                    text = line.text,
                    top = box?.top ?: 0,
                    left = box?.left ?: 0,
                    right = box?.right ?: 0,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptOcr.kt
git commit -m "feat: add ML Kit receipt OCR wrapper"
```

---

## Task 6: FileProvider for camera capture

**Files:**
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add the paths file**

Create `file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="receipts" path="receipts/" />
</paths>
```

- [ ] **Step 2: Register the provider + camera feature**

In `AndroidManifest.xml`, inside `<application>` add:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```
And before `<application>` (optional camera feature, not required):
```xml
<uses-feature android:name="android.hardware.camera.any" android:required="false" />
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/xml/file_paths.xml app/src/main/AndroidManifest.xml
git commit -m "build: add FileProvider for in-app receipt camera capture"
```

---

## Task 7: ReceiptScanViewModel

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanViewModel.kt`
- Modify: `app/src/main/java/com/nexapos/retail/ui/AppViewModelProvider.kt`

- [ ] **Step 1: Implement the ViewModel**
```kotlin
package com.nexapos.retail.ui.purchase.receipt

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.media.ImageStore
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import com.nexapos.retail.ui.purchase.PurchaseDraftItem
import com.nexapos.retail.ui.purchase.recordPurchaseFromDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanPhase { IDLE, PROCESSING, REVIEW, DONE }

class ReceiptScanViewModel(
    private val purchasesRepository: PurchasesRepository,
    private val catalogRepository: CatalogRepository,
    private val partiesRepository: PartiesRepository,
) : ViewModel() {
    var phase by mutableStateOf(ScanPhase.IDLE); private set
    var supplier by mutableStateOf("")
    var warnings by mutableStateOf<List<String>>(emptyList()); private set
    var imageName by mutableStateOf<String?>(null); private set
    val lines = mutableStateListOf<ReceiptDraftLine>()

    private var catalogNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            catalogRepository.observeAllProducts().collect { products ->
                catalogNames = products.map { it.name.lowercase() }.toSet()
            }
        }
    }

    /** Whether a line's name already exists in the catalog (else it'll be created). */
    fun isKnown(line: ReceiptDraftLine): Boolean = line.name.trim().lowercase() in catalogNames

    val total: Int get() = lines.sumOf { it.quantity * it.unitCostRupees }

    /** Runs OCR + parse on a captured image, then moves to REVIEW. */
    fun onImageCaptured(context: Context, uri: Uri) {
        phase = ScanPhase.PROCESSING
        viewModelScope.launch {
            imageName = withContext(Dispatchers.IO) { ImageStore.save(context, uri, "receipt") }
            val parsed = try {
                val ocr = ReceiptOcr.recognise(context, uri)
                ReceiptParser.parse(ocr)
            } catch (e: Exception) {
                ParsedReceipt("", emptyList(), listOf("Couldn't read the image — enter the items manually."))
            }
            supplier = parsed.supplierGuess
            warnings = parsed.warnings
            lines.clear(); lines.addAll(parsed.lines)
            phase = ScanPhase.REVIEW
        }
    }

    fun updateLine(index: Int, line: ReceiptDraftLine) { if (index in lines.indices) lines[index] = line }
    fun removeLine(index: Int) { if (index in lines.indices) lines.removeAt(index) }
    fun addBlankLine() { lines.add(ReceiptDraftLine("", 1, 0)) }
    fun reset() { phase = ScanPhase.IDLE; supplier = ""; warnings = emptyList(); imageName = null; lines.clear() }

    fun register(onDone: () -> Unit) {
        val valid = lines.filter { it.name.isNotBlank() && it.quantity > 0 && it.unitCostRupees > 0 }
        if (supplier.isBlank() || valid.isEmpty()) return
        viewModelScope.launch {
            recordPurchaseFromDraft(
                purchasesRepository, catalogRepository, partiesRepository,
                supplierName = supplier,
                paymentMethod = "cash",
                items = valid.map { PurchaseDraftItem(it.name, it.quantity, it.unitCostRupees) },
                notes = "From scanned receipt",
            )
            phase = ScanPhase.DONE
            onDone()
        }
    }
}
```

- [ ] **Step 2: Register it in the factory**

In `AppViewModelProvider.kt`, add an `initializer` block alongside the others:
```kotlin
initializer {
    val c = posApplication().container
    com.nexapos.retail.ui.purchase.receipt.ReceiptScanViewModel(
        purchasesRepository = c.purchasesRepository,
        catalogRepository = c.catalogRepository,
        partiesRepository = c.partiesRepository,
    )
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanViewModel.kt app/src/main/java/com/nexapos/retail/ui/AppViewModelProvider.kt
git commit -m "feat: add ReceiptScanViewModel (OCR + editable draft + register)"
```

---

## Task 8: ReceiptScanScreen + navigation entry

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanScreen.kt`
- Modify: `app/src/main/java/com/nexapos/retail/ui/PosApp.kt`
- Modify: `app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseScreen.kt`

- [ ] **Step 1: Build the screen**

Create `ReceiptScanScreen.kt`. Use `NavShell(active = "purchase", onNav = onNav)`, an `AppBar(title = "Scan receipt", ...)`, and three states driven by `vm.phase`:
- **IDLE:** two buttons — `PrimaryBtn(PosIcons.scan, "Take photo")` launching the camera (TakePicture into a FileProvider cache Uri), and `SecBtn(PosIcons.upload, "Pick image")` launching `GetContent("image/*")`. Both call `vm.onImageCaptured(context, uri)`.
- **PROCESSING:** a centered `CircularProgressIndicator` + "Reading receipt…".
- **REVIEW:** show the receipt thumbnail (`ImageStore.load(context, vm.imageName)`), an `EditableField` for `vm.supplier`, the `vm.warnings` (if any) in an amber note, then the editable line list — each row: name (`EditableField`), qty + unit cost (`EditableField number=true`), a "new"/"in catalog" tag via `vm.isKnown(line)`, and a remove action; an "Add line" `SecBtn`; a `SumRow("Total", rsStr(vm.total))`; and a `WideBtn("Register purchase", primary = true) { vm.register { onBack() } }`.

Camera launcher pattern:
```kotlin
val context = LocalContext.current
var pendingUri by remember { mutableStateOf<Uri?>(null) }
val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
    val uri = pendingUri
    if (ok && uri != null) vm.onImageCaptured(context, uri)
}
val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) vm.onImageCaptured(context, uri)
}
fun launchCamera() {
    val dir = java.io.File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = java.io.File(dir, "capture-${System.currentTimeMillis()}.jpg")
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file,
    )
    pendingUri = uri
    takePhoto.launch(uri)
}
```
Follow `ProductsScreen`/`PurchaseForms` for layout, spacing, `PosTheme.colors`, and the private `rsStr(...)` helper (copy the small helper locally as those screens do).

- [ ] **Step 2: Add the route in PosApp.kt**

Next to the other purchase routes, add:
```kotlin
composable("receipt-scan") {
    com.nexapos.retail.ui.purchase.receipt.ReceiptScanScreen(
        vm = viewModel(factory = AppViewModelProvider.Factory),
        onNav = go,
        onBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 3: Add the entry button on the Purchase screen**

In `PurchaseScreen.kt`, add an `onScanReceipt: () -> Unit` parameter to the composable signature, and in the AppBar `right = { Row(...) }` block (before `PrimaryBtn(... "New purchase" ...)`), add:
```kotlin
SecBtn(PosIcons.scan, "Scan receipt", onScanReceipt)
```
Then in `PosApp.kt` where `PurchaseScreen(...)` is invoked, pass `onScanReceipt = { go("receipt-scan") }`.

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nexapos/retail/ui/purchase/receipt/ReceiptScanScreen.kt app/src/main/java/com/nexapos/retail/ui/PosApp.kt app/src/main/java/com/nexapos/retail/ui/purchase/PurchaseScreen.kt
git commit -m "feat: receipt scan screen + Purchase-screen entry + route"
```

---

## Task 9: Lint, full test pass, manual verification

**Files:** none (verification).

- [ ] **Step 1: Format + static checks**

Run: `./gradlew ktlintFormat` (own invocation), then `./gradlew testDebugUnitTest detekt ktlintCheck --continue`
Expected: BUILD SUCCESSFUL; parser + recorder tests green.

- [ ] **Step 2: Install + manual emulator pass (Galaxy_Tab_A11)**

Run: `./gradlew assembleDebug` then `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Verify:
- Purchase screen → "Scan receipt" → Take photo / Pick image works.
- A printed receipt image → lines auto-fill; edit a price; "new"/"in catalog" tags correct.
- Register → returns to Purchase list with a new PO; open Products and confirm stock rose for matched items and new items were created.
- Blank/garbage image → lands in review with a warning and empty list; manual add + register still works.

- [ ] **Step 3: Commit any lint fixups**
```bash
git add -A
git commit -m "chore: ktlint/detekt fixups for receipt scan"
```

---

## Notes for the implementer
- **No DB schema change** — do not add a column to `Purchase`. The receipt photo is stored via `ImageStore` (filename kept on the draft/VM); persisting the filename per-PO long-term is a future upgrade (would need a real Room migration, explicitly out of scope).
- Keep new files under ~500 lines; `ReceiptScanScreen.kt` is the largest — split a `ReceiptLineRow` private composable if it grows.
- Money is integer cents at the DB boundary; the draft works in whole rupees (`unitCostRupees`) exactly like `PurchaseDraftItem`.
