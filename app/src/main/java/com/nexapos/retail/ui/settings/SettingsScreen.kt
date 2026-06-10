package com.nexapos.retail.ui.settings

import android.content.Intent
import android.net.Uri
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
import com.nexapos.retail.data.AppReset
import com.nexapos.retail.data.backup.BackupManager
import com.nexapos.retail.data.backup.BackupPrefs
import com.nexapos.retail.data.entity.isAdmin
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.security.DbKeyManager
import com.nexapos.retail.data.security.PinManager
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.NexaLogo
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.session.rememberIsAdmin
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SettingItem(val icon: List<String>, val label: String, val desc: String, val to: String)

private data class SettingGroup(val title: String, val items: List<SettingItem>)

private fun groups(admin: Boolean) =
    listOfNotNull(
        SettingGroup(
            "Staff",
            listOf(
                SettingItem(PosIcons.people, "Staff & roles", "Admins see profit · cashiers don't", "staff-settings"),
            ),
        ).takeIf { admin },
        SettingGroup(
            "Receipt & hardware",
            listOf(
                SettingItem(PosIcons.print, "Printing & receipt", "Paper size, footer, test print", "printing-settings"),
                SettingItem(PosIcons.barcode, "Barcode scanner", "External USB / Bluetooth scanner", "scanner-settings"),
            ),
        ),
        SettingGroup(
            "Shortcuts",
            listOf(
                SettingItem(PosIcons.wallet, "Cash & bank accounts", "Tills · banks · mobile wallets", "money"),
                SettingItem(PosIcons.chart, "Income & expenses", "Manage cash-book entries", "income"),
            ),
        ),
    )

@Composable
fun SettingsScreen(onNav: (String) -> Unit) {
    val c = PosTheme.colors
    val context = LocalContext.current
    // Admins manage data (backup/restore/delete) and staff; cashiers only
    // change their own PIN here.
    val admin = rememberIsAdmin()
    var businessName by remember { mutableStateOf(BusinessProfile.name(context)) }
    var businessAddress by remember { mutableStateOf(BusinessProfile.address(context)) }
    var businessBrn by remember { mutableStateOf(BusinessProfile.brn(context)) }
    var businessVat by remember { mutableStateOf(BusinessProfile.vat(context)) }
    var businessVatReg by remember { mutableStateOf(BusinessProfile.vatRegistered(context)) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        BusinessProfileDialog(
            initialName = businessName,
            initialAddress = businessAddress,
            initialBrn = businessBrn,
            initialVat = businessVat,
            initialVatRegistered = businessVatReg,
            onDismiss = { showProfileDialog = false },
            onSave = { n, addr, b, v, vr ->
                BusinessProfile.setProfile(context, n, addr, b, v, vr)
                businessName = BusinessProfile.name(context)
                businessAddress = BusinessProfile.address(context)
                businessBrn = BusinessProfile.brn(context)
                businessVat = BusinessProfile.vat(context)
                businessVatReg = BusinessProfile.vatRegistered(context)
                showProfileDialog = false
            },
        )
    }
    if (showDeleteDialog) {
        DeleteAllDataDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                AppReset.wipeAndRestart(context)
            },
        )
    }

    if (showHelp) {
        val cc = PosTheme.colors
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } },
            title = { Text("Help & support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NexaPOS Retail · v1.0.0", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cc.ink)
                    Text(
                        "• Sell: open POS, tap products, then Charge.\n" +
                            "• Stock: Products → Add product or Import (CSV).\n" +
                            "• Customers & suppliers: Parties.\n" +
                            "• Cash in & out and the ledger: Money.\n" +
                            "• Printable summaries: Reports (export to Excel / PDF).\n" +
                            "• Back up your data regularly under Data & security below.",
                        fontSize = 12.5.sp,
                        color = cc.muted,
                    )
                    Text(
                        "Your staff PIN unlocks the till each time the app opens. " +
                            "For setup or support, contact your installer.",
                        fontSize = 12.5.sp,
                        color = cc.muted,
                    )
                }
            },
        )
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(title = "Settings", subtitle = "$businessName · v 1.0.0", right = { SecBtn(null, "Help") { showHelp = true } })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 960.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // business profile
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NexaLogo(size = 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(businessName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                        val subtitle =
                            listOfNotNull(
                                businessAddress.takeIf { it.isNotBlank() },
                                businessBrn.takeIf { it.isNotBlank() }?.let { "BRN $it" },
                                businessVat.takeIf { it.isNotBlank() }?.let { "VAT $it" },
                            ).joinToString(" · ").ifBlank { "Tap Edit to set address, BRN and VAT" }
                        Text(
                            subtitle,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                    SecBtn(null, "Edit") { showProfileDialog = true }
                }
                // appearance
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Appearance")
                        Spacer(Modifier.height(2.dp))
                        Text("Theme", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                        Text("Daylight is the default — tuned for glare-free shop counters.", fontSize = 12.sp, color = c.muted)
                    }
                    ThemePill(PosIcons.sun, "Daylight", true)
                    ThemePill(PosIcons.moon, "Counter Mode", false)
                }
                DataSecurityCard(admin = admin)
                // groups
                groups(admin).forEach { g ->
                    Column(Modifier.fillMaxWidth()) {
                        Eyebrow(g.title)
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp))) {
                            g.items.forEachIndexed { i, item ->
                                SettingRow(item) { if (item.to.isNotEmpty()) onNav(item.to) }
                                if (i < g.items.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                            }
                        }
                    }
                }
                // danger zone — wiping the shop is an owner decision, never a cashier's
                if (admin) {
                    DangerZoneCard(onDelete = { showDeleteDialog = true })
                }
            }
        }
    }
}

@Composable
private fun DangerZoneCard(onDelete: () -> Unit) {
    val c = PosTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.crimsonSoft.copy(alpha = 0.35f)).border(1.dp, c.crimsonSoft, RoundedCornerShape(14.dp)).padding(18.dp)) {
        Eyebrow("Danger zone")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Delete business data", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Text("This will erase all sales, products, and reports for this shop. Cannot be undone.", fontSize = 12.sp, color = c.muted)
            }
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.crimson)
                    .clickable { onDelete() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PosIcon(PosIcons.trash, tint = Color.White, size = 14.dp)
                Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ThemePill(
    icon: List<String>,
    label: String,
    active: Boolean,
) {
    val c = PosTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(if (active) c.ink else c.raised).border(if (active) 1.5.dp else 1.dp, if (active) c.ink else c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosIcon(icon, tint = if (active) c.surface else c.ink, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (active) c.surface else c.ink)
    }
}

@Composable
private fun SettingRow(
    item: SettingItem,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.raised2).border(1.dp, c.hairline, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            PosIcon(item.icon, tint = c.ink, size = 16.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(item.desc, fontSize = 12.sp, color = c.muted)
        }
        PosIcon(PosIcons.chevR, tint = c.muted, size = 16.dp)
    }
}

private val whenFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

/**
 * Data & security. Admins get the full toolkit — backup, restore, recovery key,
 * delete lives just below. Cashiers only change their own sign-in PIN: restoring
 * or backing up the encrypted DB is the owner's job, not the till operator's.
 */
@Composable
private fun DataSecurityCard(admin: Boolean) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folder by remember { mutableStateOf(BackupPrefs.folderUri(context)) }
    var lastAt by remember { mutableStateOf(BackupPrefs.lastBackupAt(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPin by remember { mutableStateOf(false) }
    // Two-step flow for the recovery key: first verify PIN, then show the key.
    var showKeyPinGate by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                BackupPrefs.setFolderUri(context, uri.toString())
                folder = uri.toString()
                message = "Backup folder set."
            }
        }
    val restorePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) restoreUri = uri
        }

    @Suppress("TooGenericExceptionCaught") // backup failure (e.g. revoked SAF permission) is shown in `message`
    fun runBackup() {
        val dest = folder
        if (dest == null) {
            folderPicker.launch(null)
            return
        }
        message =
            try {
                val name = BackupManager.backupNow(context, Uri.parse(dest))
                lastAt = BackupPrefs.lastBackupAt(context)
                "Backed up: $name"
            } catch (e: IOException) {
                "Backup failed: ${e.message}"
            } catch (e: RuntimeException) {
                "Backup failed: ${e.message}"
            }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow("Data & security")
        if (admin) {
            Text("Backup & restore", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(
                if (lastAt > 0L) "Last backup: ${whenFmt.format(Date(lastAt))}" else "No backup yet. Backups are encrypted and saved to a folder you choose (USB/SD card or a Drive-synced folder).",
                fontSize = 12.sp,
                color = c.muted,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WideBtn("Backup now", primary = true, Modifier.weight(1f), icon = PosIcons.download) { runBackup() }
                WideBtn("Choose folder", primary = false, Modifier.weight(1f)) { folderPicker.launch(null) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WideBtn("Restore…", primary = false, Modifier.weight(1f)) { restorePicker.launch(arrayOf("*/*")) }
                WideBtn("Change PIN", primary = false, Modifier.weight(1f)) { showPin = true }
            }
            WideBtn("Show recovery key", primary = false, Modifier.fillMaxWidth()) { showKeyPinGate = true }
        } else {
            Text("Your PIN", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(
                "Change the PIN you sign in with. Backups, restore and data deletion are managed by an admin.",
                fontSize = 12.sp,
                color = c.muted,
            )
            WideBtn("Change PIN", primary = false, Modifier.fillMaxWidth()) { showPin = true }
        }
        message?.let { Text(it, fontSize = 12.sp, color = c.emerald) }
    }

    val container = (context.applicationContext as PosApplication).container
    if (showPin) {
        ChangePinDialog(
            onDismiss = { showPin = false },
            onSave = { pin ->
                scope.launch {
                    val staff = container.session.current.value
                    message =
                        try {
                            if (staff != null) {
                                // Signed-in staff change their own PIN (uniqueness enforced).
                                withContext(Dispatchers.Default) { container.staffRepository.setPin(staff.id, pin) }
                            } else {
                                PinManager.setPin(context, pin)
                            }
                            "PIN updated."
                        } catch (e: IllegalArgumentException) {
                            e.message
                        }
                    showPin = false
                }
            },
        )
    }

    // Recovery key: require an ADMIN PIN before revealing the passphrase —
    // it decrypts the whole database, so cashier PINs must not open it.
    if (showKeyPinGate) {
        VerifyPinDialog(
            onDismiss = { showKeyPinGate = false },
            onVerified = { enteredPin ->
                scope.launch {
                    val ok =
                        withContext(Dispatchers.Default) {
                            val staff = container.staffRepository.findByPin(enteredPin)
                            if (staff != null) staff.isAdmin() else PinManager.verify(context, enteredPin)
                        }
                    if (ok) {
                        showKeyPinGate = false
                        showKey = true
                    } else {
                        message = "Incorrect PIN — an admin PIN is required."
                        showKeyPinGate = false
                    }
                }
            },
        )
    }
    if (showKey) {
        ShowKeyDialog(key = DbKeyManager.getOrCreatePassphrase(context), onDismiss = { showKey = false })
    }
    restoreUri?.let { uri ->
        RestoreDialog(
            onDismiss = { restoreUri = null },
            onConfirm = { key ->
                if (key.isNotBlank()) DbKeyManager.setPassphrase(context, key)
                BackupManager.stageRestore(context, uri)
                BackupManager.restart(context)
            },
        )
    }
}

/**
 * Modal dialog that asks the user to confirm their current PIN before a sensitive
 * action (e.g. revealing the recovery key).  The caller is responsible for running
 * [PinManager.verify] off the main thread.
 */
@Composable
private fun VerifyPinDialog(
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onVerified(pin) }, enabled = pin.isNotBlank()) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Confirm your PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter an admin PIN to reveal the recovery key.", fontSize = 13.sp)
                EditableField("Current PIN", pin, { pin = it }, Modifier.fillMaxWidth(), mono = true, number = true, placeholder = "····")
            }
        },
    )
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..8 && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(pin) }, enabled = valid) { Text("Save PIN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Change staff PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditableField("New PIN (4–8 digits)", pin, { pin = it }, Modifier.fillMaxWidth(), mono = true, number = true, placeholder = "····")
                EditableField("Confirm PIN", confirm, { confirm = it }, Modifier.fillMaxWidth(), mono = true, number = true, placeholder = "····")
            }
        },
    )
}

@Composable
private fun ShowKeyDialog(
    key: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Backup recovery key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Write this down and keep it safe. You need it to restore a backup onto a new device.", fontSize = 13.sp)
                Text(key, fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun RestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(key) }) { Text("Restore & restart") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Restore backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This replaces all current data with the chosen backup, then restarts the app.", fontSize = 13.sp)
                Text("If the backup is from a different device, enter its recovery key. Leave blank if it's from this device.", fontSize = 12.sp)
                EditableField("Recovery key (optional)", key, { key = it }, Modifier.fillMaxWidth(), mono = true, placeholder = "key…")
            }
        },
    )
}

@Composable
private fun BusinessProfileDialog(
    initialName: String,
    initialAddress: String,
    initialBrn: String,
    initialVat: String,
    initialVatRegistered: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit,
) {
    val c = PosTheme.colors
    var name by remember { mutableStateOf(initialName.takeIf { it != BusinessProfile.DEFAULT_NAME } ?: "") }
    var address by remember { mutableStateOf(initialAddress) }
    var brn by remember { mutableStateOf(initialBrn) }
    var vat by remember { mutableStateOf(initialVat) }
    var vatRegistered by remember { mutableStateOf(initialVatRegistered) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(name, address, brn, vat, vatRegistered) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Business profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Shown on the Settings screen and printed at the top of every receipt.", fontSize = 12.sp)
                EditableField("Business name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "QUINCAILLERIE RB TRADING")
                EditableField("Address", address, { address = it }, Modifier.fillMaxWidth(), placeholder = "Royal Rd, Curepipe")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditableField("BRN", brn, { brn = it }, Modifier.weight(1f), mono = true, placeholder = "C20177445")
                    EditableField("VAT number", vat, { vat = it }, Modifier.weight(1f), mono = true, placeholder = "VAT20188822")
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("VAT-registered", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("Off = no VAT on sales, receipts or the VAT number", fontSize = 11.sp, color = c.muted)
                    }
                    Switch(
                        checked = vatRegistered,
                        onCheckedChange = { vatRegistered = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = Color.White),
                    )
                }
            }
        },
    )
}

@Composable
private fun DeleteAllDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val canDelete = typed.equals("DELETE", ignoreCase = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canDelete) { Text("Delete everything") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Delete all business data?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This permanently erases the encrypted database, product photos, your PIN, and the business profile, then restarts the app fresh.", fontSize = 13.sp)
                Text("Type DELETE to confirm.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                EditableField("Confirmation", typed, { typed = it }, Modifier.fillMaxWidth(), mono = true, placeholder = "DELETE")
            }
        },
    )
}
