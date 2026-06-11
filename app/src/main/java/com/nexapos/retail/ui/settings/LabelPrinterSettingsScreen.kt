package com.nexapos.retail.ui.settings

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.hardware.drawer.BluetoothDrawerTransport
import com.nexapos.retail.data.hardware.labels.Tspl
import com.nexapos.retail.data.profile.LabelPrinterSettings
import com.nexapos.retail.domain.hardware.LabelSpec
import com.nexapos.retail.domain.hardware.PrintOutcome
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.launch

private data class PairedLabelPrinter(val name: String, val mac: String)

private val TEST_SPEC = LabelSpec(name = "NexaPOS test label", sku = "TEST-01", barcode = "2000000000008")

@Composable
fun LabelPrinterSettingsScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printer = (context.applicationContext as PosApplication).container.labelPrinter

    var transport by remember { mutableStateOf(LabelPrinterSettings.transport(context)) }
    var btMac by remember { mutableStateOf(LabelPrinterSettings.btMac(context)) }
    var btName by remember { mutableStateOf(LabelPrinterSettings.btName(context)) }
    var lanHost by remember { mutableStateOf(LabelPrinterSettings.lanHost(context)) }
    var lanPort by remember { mutableStateOf(LabelPrinterSettings.lanPort(context).toString()) }
    var sizePreset by remember { mutableStateOf(LabelPrinterSettings.sizePreset(context)) }
    var showPrice by remember { mutableStateOf(LabelPrinterSettings.showPrice(context)) }
    var saved by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showCommands by remember { mutableStateOf(false) }
    var btPermission by remember { mutableStateOf(BluetoothDrawerTransport.hasConnectPermission(context)) }
    var pairedDevices by remember { mutableStateOf(pairedLabelPrinters(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            btPermission = granted
            if (granted) pairedDevices = pairedLabelPrinters(context)
        }

    fun persist() {
        LabelPrinterSettings.setTransport(context, transport)
        LabelPrinterSettings.setBtDevice(context, btMac, btName)
        LabelPrinterSettings.setLan(context, lanHost, lanPort.toIntOrNull() ?: LabelPrinterSettings.DEFAULT_LAN_PORT)
        LabelPrinterSettings.setSizePreset(context, sizePreset)
        LabelPrinterSettings.setShowPrice(context, showPrice)
        saved = true
    }

    if (showCommands) {
        AlertDialog(
            onDismissRequest = { showCommands = false },
            confirmButton = { TextButton(onClick = { showCommands = false }) { Text("Close") } },
            title = { Text("TSPL commands (test label)") },
            text = {
                Text(
                    Tspl.labelText(TEST_SPEC, sizePreset.size),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = c.ink,
                )
            },
        )
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Label printer",
            subtitle = "Thermal barcode-label printer (TSPL)",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabelCard {
                    Eyebrow("How it works")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Connect a thermal LABEL printer (XPrinter, TSC, HPRT, Gprinter…) loaded with " +
                            "sticker rolls. Products → Print labels then prints Name + SKU + barcode " +
                            "stickers, as many copies per item as you choose.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                LabelCard {
                    Eyebrow("Printer connection")
                    Spacer(Modifier.height(8.dp))
                    LabelPrinterSettings.Transport.entries.forEach { opt ->
                        LabelRadioRow(label = opt.label, selected = transport == opt) {
                            transport = opt
                            saved = false
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (transport) {
                        LabelPrinterSettings.Transport.BLUETOOTH -> {
                            if (!btPermission) {
                                Text(
                                    "NexaPOS needs Bluetooth permission to list your paired printers.",
                                    fontSize = 12.sp,
                                    color = c.muted,
                                )
                                Spacer(Modifier.height(8.dp))
                                WideBtn("Allow Bluetooth", primary = false, Modifier.fillMaxWidth()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                }
                            } else if (pairedDevices.isEmpty()) {
                                Text(
                                    "No paired Bluetooth devices. Pair the label printer in Android Settings → " +
                                        "Connected devices first, then come back here.",
                                    fontSize = 12.sp,
                                    color = c.muted,
                                )
                            } else {
                                Text("Pick your label printer:", fontSize = 12.sp, color = c.muted)
                                Spacer(Modifier.height(4.dp))
                                pairedDevices.forEach { device ->
                                    LabelRadioRow(
                                        label = "${device.name}  ·  ${device.mac}",
                                        selected = btMac == device.mac,
                                    ) {
                                        btMac = device.mac
                                        btName = device.name
                                        saved = false
                                    }
                                }
                            }
                        }
                        LabelPrinterSettings.Transport.LAN -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditableField(
                                    "Printer IP / hostname",
                                    lanHost,
                                    {
                                        lanHost = it
                                        saved = false
                                    },
                                    Modifier.weight(2f),
                                    mono = true,
                                    placeholder = "192.168.100.60",
                                )
                                EditableField(
                                    "Port",
                                    lanPort,
                                    {
                                        lanPort = it
                                        saved = false
                                    },
                                    Modifier.weight(1f),
                                    mono = true,
                                    number = true,
                                    placeholder = "9100",
                                )
                            }
                        }
                    }
                }

                LabelCard {
                    Eyebrow("Label size")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Match the sticker roll loaded in the printer.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    LabelPrinterSettings.SizePreset.entries.forEach { opt ->
                        LabelRadioRow(label = opt.label, selected = sizePreset == opt) {
                            sizePreset = opt
                            saved = false
                        }
                    }
                }

                LabelCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Price on labels", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("Also print the selling price under the SKU", fontSize = 11.sp, color = c.muted)
                        }
                        Switch(
                            checked = showPrice,
                            onCheckedChange = {
                                showPrice = it
                                saved = false
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = Color.White),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WideBtn("Print test label", primary = false, Modifier.weight(1f)) {
                        persist()
                        testResult = "Printing test label…"
                        scope.launch {
                            testResult =
                                when (val r = printer.print(listOf(TEST_SPEC))) {
                                    is PrintOutcome.Done -> "Sent — a test label should have printed."
                                    is PrintOutcome.FailedAt -> "Failed: ${r.reason}"
                                }
                        }
                    }
                    WideBtn(if (saved) "Saved ✓" else "Save", primary = true, Modifier.weight(1f), icon = PosIcons.check) {
                        persist()
                    }
                }
                testResult?.let {
                    Text(it, fontSize = 13.sp, fontFamily = JetBrainsMono, color = if (it.startsWith("Failed")) c.crimson else c.emerald)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { showCommands = true }.padding(8.dp),
                ) {
                    Text("View the raw TSPL commands (debug)", fontSize = 11.sp, color = c.muted)
                }
            }
        }
    }
}

/** Bonded classic-BT devices, or empty when permission/adapter is missing. */
private fun pairedLabelPrinters(context: Context): List<PairedLabelPrinter> {
    if (!BluetoothDrawerTransport.hasConnectPermission(context)) return emptyList()
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return emptyList()
    return try {
        adapter.bondedDevices.orEmpty().map { PairedLabelPrinter(it.name ?: "Unknown device", it.address) }
    } catch (_: SecurityException) {
        emptyList()
    }
}

@Composable
private fun LabelCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
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
private fun LabelRadioRow(
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
