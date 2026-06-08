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
import com.nexapos.retail.ui.components.PosIcons
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

                WideBtn(if (saved) "Saved ✓" else "Save", primary = true, Modifier.fillMaxWidth(), icon = PosIcons.check) {
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
