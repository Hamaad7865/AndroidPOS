# External (HID) Barcode Scanner — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a physical USB/Bluetooth (HID) barcode scanner add products to the POS cart and fill the product barcode field, without breaking normal keyboard typing.

**Architecture:** An HID scanner is an OS keyboard whose keystrokes reach `MainActivity` before Compose. `MainActivity.dispatchKeyEvent` feeds keys to a pure `BarcodeAssembler` (timing + terminator → completed code); the code is published on a process-wide `ScannerEvents` `SharedFlow` that the active screen collects and routes into the existing `addByBarcode` / barcode-field logic. Settings gate it via a `ScannerInput` prefs object.

**Tech Stack:** Kotlin, Jetpack Compose, Android `KeyEvent`, `kotlinx.coroutines` `SharedFlow`, JUnit4. Spec: `docs/superpowers/specs/2026-06-08-external-scanner-design.md`.

**Branch:** `feature/external-scanner`. Build: `JAVA_HOME=D:/Android/jdk17 ./gradlew …`. Run `ktlintFormat` in its own invocation before checks.

---

## File structure

**Create**
- `app/src/main/java/com/nexapos/retail/data/barcode/BarcodeAssembler.kt` — pure burst→barcode logic (unit-tested).
- `app/src/main/java/com/nexapos/retail/data/barcode/ScannerEvents.kt` — app-level `SharedFlow<String>` bus (object singleton, matching `BarcodeScanner`'s object style).
- `app/src/main/java/com/nexapos/retail/data/barcode/ScannerBridge.kt` — `KeyEvent` → `BarcodeAssembler` adapter; decides what to swallow.
- `app/src/main/java/com/nexapos/retail/data/profile/ScannerInput.kt` — prefs (enabled + terminator), mirrors `ReceiptSettings`.
- `app/src/main/java/com/nexapos/retail/ui/settings/ScannerSettingsScreen.kt` — settings sub-screen (toggle + terminator).
- `app/src/test/java/com/nexapos/retail/data/barcode/BarcodeAssemblerTest.kt` — unit tests.

**Modify**
- `app/src/main/java/com/nexapos/retail/MainActivity.kt` — `dispatchKeyEvent` override + a `ScannerBridge`.
- `app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt` — collect `ScannerEvents.scans` → `addByBarcode`.
- `app/src/main/java/com/nexapos/retail/ui/products/ProductsScreen.kt` — collect `ScannerEvents.scans` → `barcode = code`.
- `app/src/main/java/com/nexapos/retail/ui/settings/SettingsScreen.kt` — add a "Barcode scanner" `SettingItem`.
- `app/src/main/java/com/nexapos/retail/ui/PosApp.kt` — register the `scanner-settings` route.

> **Design note (deviation from spec):** the spec said `AppContainer` owns the event bus; we use a Kotlin `object ScannerEvents` instead — `MainActivity` doesn't currently touch `AppContainer`, and an `object` matches the existing `BarcodeScanner`/`ReceiptSettings`/`BusinessProfile` singleton style. Same architecture (one app-level `SharedFlow`), less wiring.

> **Two thresholds (why):** `RESET_GAP_MS = 100` assembles a scanner burst (resets the buffer on human-speed gaps, so manual typing never accumulates into a "scan"). `SWALLOW_GAP_MS = 40` decides which keystrokes to *hide* from focused fields — only chars arriving faster than any human (≤40 ms) are swallowed, so **typing is never eaten**; the first char of a burst and slow-scanner chars may briefly appear in a focused field (cosmetic; POS clears it on a hit).

---

## Task 1: BarcodeAssembler (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/data/barcode/BarcodeAssembler.kt`
- Test: `app/src/test/java/com/nexapos/retail/data/barcode/BarcodeAssemblerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.nexapos.retail.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeAssemblerTest {
    private fun assembler() = BarcodeAssembler()

    @Test
    fun `fast burst then terminator emits the code`() {
        val a = assembler()
        assertFalse(a.feed('5', 1000)) // first char never swallowed
        assertTrue(a.feed('9', 1005)) // machine-fast → swallowed
        assertTrue(a.feed('0', 1010))
        assertTrue(a.feed('1', 1015))
        assertEquals("5901", a.finish(1020))
    }

    @Test
    fun `slow human typing then enter is not a scan and is never swallowed`() {
        val a = assembler()
        assertFalse(a.feed('1', 1000))
        assertFalse(a.feed('2', 1200)) // 200ms gap → buffer reset, not swallowed
        assertFalse(a.feed('3', 1400))
        assertNull(a.finish(1600))
    }

    @Test
    fun `burst shorter than the minimum length is rejected`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        assertNull(a.finish(1010)) // length 2 < MIN_LEN 3
    }

    @Test
    fun `a mid-burst pause drops the earlier characters`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        a.feed('3', 1300) // 295ms gap → reset
        a.feed('4', 1305)
        a.feed('5', 1310)
        assertEquals("345", a.finish(1315))
    }

    @Test
    fun `a slow terminator after a fast burst is rejected`() {
        val a = assembler()
        a.feed('1', 1000)
        a.feed('2', 1005)
        a.feed('3', 1010)
        assertNull(a.finish(1300)) // 290ms gap before terminator
    }

    @Test
    fun `terminator with an empty buffer yields null`() {
        assertNull(assembler().finish(1000))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*BarcodeAssemblerTest*"`
Expected: FAIL — `BarcodeAssembler` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.nexapos.retail.data.barcode

/**
 * Pure assembler that turns a stream of typed characters + a terminator into a
 * barcode, distinguishing a hardware scanner's fast burst from human typing.
 *
 * - [feed] appends a character. A gap longer than [resetGapMs] since the previous
 *   character resets the buffer, so slow human typing never accumulates into a
 *   "scan". It returns true when the character should be SWALLOWED (hidden from a
 *   focused field) — only for machine-fast chars (gap <= [swallowGapMs]) after the
 *   first — so normal typing is never eaten.
 * - [finish] is called on a terminator key; it returns the code iff the buffer was
 *   built as a fast burst of at least [minLen] characters, then resets.
 *
 * No Android types → fully unit-testable.
 */
class BarcodeAssembler(
    private val resetGapMs: Long = RESET_GAP_MS,
    private val swallowGapMs: Long = SWALLOW_GAP_MS,
    private val minLen: Int = MIN_LEN,
) {
    private val buffer = StringBuilder()
    private var lastMs = 0L

    fun feed(
        ch: Char,
        atMs: Long,
    ): Boolean {
        val gap = atMs - lastMs
        if (gap > resetGapMs) buffer.setLength(0)
        buffer.append(ch)
        lastMs = atMs
        return buffer.length > 1 && gap in 0..swallowGapMs
    }

    fun finish(atMs: Long): String? {
        val fast = atMs - lastMs <= resetGapMs
        val code = buffer.toString()
        buffer.setLength(0)
        lastMs = 0L
        return code.takeIf { fast && it.length >= minLen }
    }

    companion object {
        const val RESET_GAP_MS = 100L
        const val SWALLOW_GAP_MS = 40L
        const val MIN_LEN = 3
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest --tests "*BarcodeAssemblerTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/data/barcode/BarcodeAssembler.kt app/src/test/java/com/nexapos/retail/data/barcode/BarcodeAssemblerTest.kt
git commit -m "feat: pure BarcodeAssembler for HID scanner burst detection"
```

---

## Task 2: ScannerInput prefs

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/data/profile/ScannerInput.kt`

- [ ] **Step 1: Write the file** (mirrors `ReceiptSettings`)

```kotlin
package com.nexapos.retail.data.profile

import android.content.Context

/**
 * External (USB/Bluetooth HID) barcode-scanner preferences, configured in
 * Settings → Barcode scanner. Read on every hardware key event in MainActivity.
 */
object ScannerInput {
    private const val PREFS = "nexapos_scanner"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TERMINATOR = "terminator"

    /** Which key a scanner sends after the barcode. Most send Enter; some send Tab. */
    enum class Terminator(val id: String, val label: String) {
        ENTER("enter", "Enter only"),
        TAB("tab", "Tab only"),
        BOTH("both", "Either (Enter or Tab)"),
        ;

        companion object {
            fun from(id: String?): Terminator = entries.firstOrNull { it.id == id } ?: BOTH
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun terminator(context: Context): Terminator =
        Terminator.from(prefs(context).getString(KEY_TERMINATOR, Terminator.BOTH.id))

    fun setEnabled(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun setTerminator(
        context: Context,
        value: Terminator,
    ) = prefs(context).edit().putString(KEY_TERMINATOR, value.id).apply()
}
```

- [ ] **Step 2: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/data/profile/ScannerInput.kt
git commit -m "feat: ScannerInput prefs (enabled + terminator)"
```

---

## Task 3: ScannerEvents bus

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/data/barcode/ScannerEvents.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.nexapos.retail.data.barcode

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide one-shot bus carrying a completed barcode from MainActivity's key
 * handler to whichever screen is currently composed. replay = 0 so a scan is
 * delivered once (never re-played on recomposition); a small extra buffer lets
 * [tryEmit] succeed from the non-suspending key-event path.
 */
object ScannerEvents {
    private val _scans = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val scans: SharedFlow<String> = _scans.asSharedFlow()

    fun tryEmit(code: String) {
        _scans.tryEmit(code)
    }
}
```

- [ ] **Step 2: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/data/barcode/ScannerEvents.kt
git commit -m "feat: ScannerEvents app-level SharedFlow bus"
```

---

## Task 4: ScannerBridge (KeyEvent adapter)

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/data/barcode/ScannerBridge.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.nexapos.retail.data.barcode

import android.view.KeyEvent
import com.nexapos.retail.data.profile.ScannerInput

/**
 * Adapts hardware [KeyEvent]s into the pure [BarcodeAssembler] and decides what to
 * swallow. Held by MainActivity; [emit] publishes a completed barcode, [terminator]
 * supplies the user's configured terminator key(s).
 *
 * Returns true only to SWALLOW an event from the rest of the app:
 *  - machine-fast burst characters (so they don't leak into a focused field),
 *  - the terminator (down + up) of a recognised scan.
 * Everything else returns false and reaches Compose/text fields unchanged.
 */
class ScannerBridge(
    private val emit: (String) -> Unit,
    private val terminator: () -> ScannerInput.Terminator,
) {
    private val assembler = BarcodeAssembler()
    private var swallowNextUp = false

    fun feed(event: KeyEvent): Boolean =
        when (event.action) {
            KeyEvent.ACTION_UP -> {
                val swallow = swallowNextUp && isTerminator(event.keyCode)
                if (swallow) swallowNextUp = false
                swallow
            }
            KeyEvent.ACTION_DOWN ->
                if (isTerminator(event.keyCode)) {
                    val code = assembler.finish(event.eventTime)
                    if (code != null) {
                        emit(code)
                        swallowNextUp = true
                        true
                    } else {
                        false
                    }
                } else {
                    val ch = event.unicodeChar
                    if (ch == 0) false else assembler.feed(ch.toChar(), event.eventTime)
                }
            else -> false
        }

    private fun isTerminator(keyCode: Int): Boolean {
        val enter = keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        val tab = keyCode == KeyEvent.KEYCODE_TAB
        return when (terminator()) {
            ScannerInput.Terminator.ENTER -> enter
            ScannerInput.Terminator.TAB -> tab
            ScannerInput.Terminator.BOTH -> enter || tab
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/data/barcode/ScannerBridge.kt
git commit -m "feat: ScannerBridge KeyEvent adapter for HID scanning"
```

---

## Task 5: MainActivity wiring

**Files:**
- Modify: `app/src/main/java/com/nexapos/retail/MainActivity.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.nexapos.retail

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexapos.retail.data.barcode.ScannerBridge
import com.nexapos.retail.data.barcode.ScannerEvents
import com.nexapos.retail.data.profile.ScannerInput
import com.nexapos.retail.ui.PosApp
import com.nexapos.retail.ui.theme.NexaPosTheme

class MainActivity : ComponentActivity() {
    private val scannerBridge =
        ScannerBridge(
            emit = { code -> ScannerEvents.tryEmit(code) },
            terminator = { ScannerInput.terminator(this) },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexaPosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PosApp()
                }
            }
        }
    }

    // Hardware (USB/Bluetooth HID) barcode scanners deliver the barcode as fast
    // keystrokes + a terminator. Intercept them here before Compose; swallow only a
    // completed scan so normal typing (PIN, search, forms) is unaffected.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ScannerInput.enabled(this) && scannerBridge.feed(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
```

- [ ] **Step 2: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/MainActivity.kt
git commit -m "feat: route hardware key bursts through ScannerBridge in MainActivity"
```

---

## Task 6: PosSaleScreen collector

**Files:**
- Modify: `app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt`

- [ ] **Step 1: Add imports** (top import block)

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.nexapos.retail.data.barcode.ScannerEvents
```
(If `LaunchedEffect` / `LocalContext` are already imported, skip those lines.)

- [ ] **Step 2: Add the collector** after the derived totals (after `val total = vm.total` at ~line 108, before `NavShell(`):

```kotlin
        // Hardware barcode scanner: add the scanned product to the ticket. Mirrors the
        // camera Scan button; clears the search box if any burst chars leaked into it.
        val scanContext = LocalContext.current
        LaunchedEffect(Unit) {
            ScannerEvents.scans.collect { code ->
                if (vm.addByBarcode(code)) {
                    query = ""
                } else {
                    android.widget.Toast.makeText(
                        scanContext,
                        "No product matches $code — add it first or search by name.",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                    query = code
                }
            }
        }
```

- [ ] **Step 3: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/ui/sale/PosSaleScreen.kt
git commit -m "feat: POS adds hardware-scanned products to the ticket"
```

---

## Task 7: AddProductScreen collector

**Files:**
- Modify: `app/src/main/java/com/nexapos/retail/ui/products/ProductsScreen.kt`

- [ ] **Step 1: Add the import** (top import block)

```kotlin
import com.nexapos.retail.data.barcode.ScannerEvents
```

- [ ] **Step 2: Add the collector** inside `AddProductScreen`, immediately after the existing "Load existing product" `LaunchedEffect(productId) { … }` block (ends ~line 650). `LaunchedEffect` is already imported and used here.

```kotlin
    // Hardware barcode scanner fills the barcode field, even when it isn't focused.
    LaunchedEffect(Unit) {
        ScannerEvents.scans.collect { code ->
            if (code.isNotBlank()) barcode = code
        }
    }
```

- [ ] **Step 3: Build**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/ui/products/ProductsScreen.kt
git commit -m "feat: hardware scan fills the product barcode field"
```

---

## Task 8: Scanner settings screen + entry + route

**Files:**
- Create: `app/src/main/java/com/nexapos/retail/ui/settings/ScannerSettingsScreen.kt`
- Modify: `app/src/main/java/com/nexapos/retail/ui/settings/SettingsScreen.kt` (the `groups()` list)
- Modify: `app/src/main/java/com/nexapos/retail/ui/PosApp.kt` (NavHost route)

- [ ] **Step 1: Create `ScannerSettingsScreen.kt`** (modeled on `PrintingSettingsScreen`; self-contained `Card`/`RadioRow`)

```kotlin
package com.nexapos.retail.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.profile.ScannerInput
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun ScannerSettingsScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ScannerInput.enabled(context)) }
    var terminator by remember { mutableStateOf(ScannerInput.terminator(context)) }
    var saved by remember { mutableStateOf(false) }

    fun persist() {
        ScannerInput.setEnabled(context, enabled)
        ScannerInput.setTerminator(context, terminator)
        saved = true
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Barcode scanner",
            subtitle = "Use a USB or Bluetooth scanner at the counter",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card {
                    Eyebrow("How it works")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Plug in a USB barcode scanner or pair a Bluetooth one — no setup needed. On the " +
                            "POS screen a scan adds the item to the current ticket; on Add / Edit product it fills " +
                            "the barcode field. Typing your PIN and searching still work normally.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                Card {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("External barcode scanner", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("Capture scans from a hardware scanner", fontSize = 12.sp, color = c.muted)
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                saved = false
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = Color.White),
                        )
                    }
                }

                Card {
                    Eyebrow("Terminator key")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The key your scanner sends after each barcode. Most send Enter. Leave on " +
                            "“Either” if you're not sure (check the scanner's manual to change it).",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    ScannerInput.Terminator.entries.forEach { opt ->
                        RadioRow(label = opt.label, selected = terminator == opt) {
                            terminator = opt
                            saved = false
                        }
                    }
                }

                WideBtn(if (saved) "Saved ✓" else "Save", primary = true, Modifier.fillMaxWidth()) {
                    persist()
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (selected) c.amber else c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.amber))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.ink)
    }
}
```

> **Verify before building:** confirm `WideBtn` has an overload `WideBtn(label, primary, modifier, onClick)` (used in SettingsScreen). If its signature requires an `icon`, pass `icon = PosIcons.check` (import `com.nexapos.retail.ui.components.PosIcons`). Check `PrintingSettingsScreen.kt:135` for the exact signature.

- [ ] **Step 2: Add the Settings entry** — in `SettingsScreen.kt`, inside `groups()`, add to the "Receipt & hardware" group's item list (after the Printing item, ~line 74):

```kotlin
                SettingItem(PosIcons.barcode, "Barcode scanner", "External USB / Bluetooth scanner", "scanner-settings"),
```

- [ ] **Step 3: Register the route** — in `PosApp.kt`, after the `printing-settings` composable (~line 314):

```kotlin
        composable("scanner-settings") {
            ScannerSettingsScreen(onNav = go, onBack = { navController.popBackStack() })
        }
```
Add the import: `import com.nexapos.retail.ui.settings.ScannerSettingsScreen` (or rely on the existing wildcard/explicit settings imports — match how `PrintingSettingsScreen` is imported in PosApp).

- [ ] **Step 4: Format, build, lint**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew ktlintFormat` then `JAVA_HOME=D:/Android/jdk17 ./gradlew assembleDebug detekt ktlintCheck`
Expected: BUILD SUCCESSFUL. Fix any detekt findings (e.g. `@Suppress` with justification per house style).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nexapos/retail/ui/settings/ScannerSettingsScreen.kt app/src/main/java/com/nexapos/retail/ui/settings/SettingsScreen.kt app/src/main/java/com/nexapos/retail/ui/PosApp.kt
git commit -m "feat: Settings screen for the external barcode scanner"
```

---

## Task 9: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Full check suite**

Run: `JAVA_HOME=D:/Android/jdk17 ./gradlew testDebugUnitTest detekt ktlintCheck --continue`
Expected: BUILD SUCCESSFUL; `BarcodeAssemblerTest` green.

- [ ] **Step 2: Manual on emulator/device**

Install `app-debug.apk`. With a real or simulated HID scanner (`adb shell input keyboard text "5901234123457"` followed by `adb shell input keyevent 66` approximates a scan, though slower than real hardware — for a true test use a real scanner):
- POS screen: a scan adds the matching product to the ticket (unknown code → "No product matches" toast + search filled).
- Add/Edit Product: a scan fills the barcode field.
- PIN entry and the POS search box: typing still works normally.
- Settings → Barcode scanner: toggling Off stops capture; terminator choice persists.

- [ ] **Step 3: Update the session log & push**

Update `docs/SESSION_LOG_v1.1.0.md` (mark the scanner done) and:
```bash
git push -u origin feature/external-scanner
gh pr create --base main --head feature/external-scanner --title "External (USB/Bluetooth) barcode scanner" --body "…"
```

---

## Self-review

- **Spec coverage:** intercept point (Task 5) ✓; burst assembly + human-typing distinction (Task 1) ✓; SharedFlow delivery (Task 3) ✓; POS routing (Task 6) ✓; product-field routing (Task 7) ✓; Settings toggle + terminator (Tasks 2, 8) ✓; unknown-barcode reuse (Task 6) ✓; disabled short-circuit (Task 5) ✓.
- **Placeholders:** none — every code step is complete (Task 9 PR body left as "…" intentionally, written at push time).
- **Type consistency:** `ScannerInput.Terminator` (ENTER/TAB/BOTH) used identically in `ScannerBridge`, `ScannerInput`, `ScannerSettingsScreen`; `ScannerEvents.scans`/`tryEmit`, `BarcodeAssembler.feed`/`finish`, `ScannerBridge(emit, terminator).feed(event)` consistent across tasks.
- **Open risk flagged in plan:** the `WideBtn` signature check in Task 8 Step 1; verify against `PrintingSettingsScreen.kt:135` before building.
