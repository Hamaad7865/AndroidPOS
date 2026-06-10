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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.nexapos.retail.data.hardware.drawer.DrawerPin
import com.nexapos.retail.data.profile.DrawerSettings
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.domain.hardware.KickResult
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

private data class PairedPrinter(val name: String, val mac: String)

@Composable
fun DrawerSettingsScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kicker = (context.applicationContext as PosApplication).container.drawerKicker

    var enabled by remember { mutableStateOf(DrawerSettings.enabled(context)) }
    var transport by remember { mutableStateOf(DrawerSettings.transport(context)) }
    var btMac by remember { mutableStateOf(DrawerSettings.btMac(context)) }
    var btName by remember { mutableStateOf(DrawerSettings.btName(context)) }
    var lanHost by remember { mutableStateOf(DrawerSettings.lanHost(context)) }
    var lanPort by remember { mutableStateOf(DrawerSettings.lanPort(context).toString()) }
    var pin by remember { mutableStateOf(DrawerSettings.pin(context)) }
    var kickOnSale by remember { mutableStateOf(DrawerSettings.kickOnCashSale(context)) }
    var kickOnRefund by remember { mutableStateOf(DrawerSettings.kickOnCashRefund(context)) }
    var saved by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var btPermission by remember { mutableStateOf(BluetoothDrawerTransport.hasConnectPermission(context)) }
    var pairedDevices by remember { mutableStateOf(pairedPrinters(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            btPermission = granted
            if (granted) pairedDevices = pairedPrinters(context)
        }

    fun persist() {
        DrawerSettings.setEnabled(context, enabled)
        DrawerSettings.setTransport(context, transport)
        DrawerSettings.setBtDevice(context, btMac, btName)
        DrawerSettings.setLan(context, lanHost, lanPort.toIntOrNull() ?: DrawerSettings.DEFAULT_LAN_PORT)
        DrawerSettings.setPin(context, pin)
        DrawerSettings.setKickOnCashSale(context, kickOnSale)
        DrawerSettings.setKickOnCashRefund(context, kickOnRefund)
        saved = true
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Cash drawer",
            subtitle = "Opens via the receipt printer it's plugged into",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DrawerCard {
                    Eyebrow("How it works")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The drawer's cable (RJ11) plugs into your thermal receipt printer. NexaPOS sends the " +
                            "printer a pulse command and the printer pops the drawer — so what you connect here " +
                            "is the PRINTER, over Bluetooth or your shop network.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                DrawerCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Cash drawer", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("Pop the drawer automatically on cash payments", fontSize = 12.sp, color = c.muted)
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

                DrawerCard {
                    Eyebrow("Printer connection")
                    Spacer(Modifier.height(8.dp))
                    DrawerSettings.Transport.entries.forEach { opt ->
                        DrawerRadioRow(label = opt.label, selected = transport == opt) {
                            transport = opt
                            saved = false
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (transport) {
                        DrawerSettings.Transport.BLUETOOTH -> {
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
                                    "No paired Bluetooth devices. Pair the printer in Android Settings → " +
                                        "Connected devices first, then come back here.",
                                    fontSize = 12.sp,
                                    color = c.muted,
                                )
                            } else {
                                Text("Pick your receipt printer:", fontSize = 12.sp, color = c.muted)
                                Spacer(Modifier.height(4.dp))
                                pairedDevices.forEach { device ->
                                    DrawerRadioRow(
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
                        DrawerSettings.Transport.LAN -> {
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
                                    placeholder = "192.168.100.50",
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

                DrawerCard {
                    Eyebrow("When to open")
                    Spacer(Modifier.height(6.dp))
                    DrawerToggleRow("On cash sale", "Pops as the sale completes, to take money and give change", kickOnSale) {
                        kickOnSale = it
                        saved = false
                    }
                    DrawerToggleRow("On cash refund", "Pops when a cash refund is recorded", kickOnRefund) {
                        kickOnRefund = it
                        saved = false
                    }
                }

                DrawerCard {
                    Eyebrow("Drawer pin")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Which pin of the printer's drawer port fires the pulse. Leave on Pin 2 unless the drawer doesn't react.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    DrawerPin.entries.forEach { opt ->
                        DrawerRadioRow(label = opt.label, selected = pin == opt) {
                            pin = opt
                            saved = false
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WideBtn("Test drawer", primary = false, Modifier.weight(1f)) {
                        persist()
                        testResult = "Sending pulse…"
                        scope.launch {
                            testResult =
                                when (val r = kicker.kickNow(KickReason.TEST)) {
                                    is KickResult.Sent -> "Pulse sent — the drawer should have popped."
                                    is KickResult.NotConfigured -> "Drawer is disabled or no printer is selected."
                                    is KickResult.Failed -> "Failed: ${r.message}"
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
            }
        }
    }
}

/** Bonded classic-BT devices, or empty when permission/adapter is missing. */
private fun pairedPrinters(context: Context): List<PairedPrinter> {
    if (!BluetoothDrawerTransport.hasConnectPermission(context)) return emptyList()
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return emptyList()
    return try {
        adapter.bondedDevices.orEmpty().map { PairedPrinter(it.name ?: "Unknown device", it.address) }
    } catch (_: SecurityException) {
        emptyList()
    }
}

@Composable
private fun DrawerCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
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
private fun DrawerRadioRow(
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

@Composable
private fun DrawerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text(subtitle, fontSize = 11.sp, color = c.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = Color.White),
        )
    }
}
