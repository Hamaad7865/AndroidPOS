package com.nexapos.retail.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.data.entity.isAdmin
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.session.currentStaff
import com.nexapos.retail.ui.session.rememberIsAdmin
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.delay

private const val MESSAGE_AUTO_DISMISS_MS = 4_000L

/**
 * Settings → Staff & roles. Admin-only: add cashiers/admins, rename, change
 * role, reset a PIN, deactivate. The repository enforces PIN uniqueness and
 * the "at least one active admin" rule; violations surface as the message line.
 */
@Composable
fun StaffScreen(
    vm: StaffViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val admin = rememberIsAdmin()
    val me = currentStaff()
    val staffList by vm.staff.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Staff?>(null) }

    if (showAdd) {
        AddStaffDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, pin, role ->
                vm.addStaff(name, pin, role)
                showAdd = false
            },
        )
    }
    editing?.let { staff ->
        EditStaffDialog(
            staff = staff,
            isSelf = staff.id == me?.id,
            onDismiss = { editing = null },
            onSave = { name, role, newPin ->
                if (name != staff.name) vm.rename(staff, name)
                if (role.name != staff.role) vm.setRole(staff, role)
                if (newPin != null) vm.resetPin(staff, newPin)
                editing = null
            },
            onToggleActive = {
                vm.setActive(staff, !staff.active)
                editing = null
            },
        )
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Staff & roles",
            subtitle = "Admins see everything · cashiers never see profit",
            right = { SecBtn(null, "‹ Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!admin) {
                    NoticeCard("Only admins can manage staff. Ask an admin to sign in.")
                    return@Column
                }
                vm.message?.let { msg ->
                    LaunchedEffect(msg) {
                        delay(MESSAGE_AUTO_DISMISS_MS)
                        vm.clearMessage()
                    }
                    Text(msg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.emerald)
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
                ) {
                    staffList.forEachIndexed { i, staff ->
                        StaffRow(staff = staff, isSelf = staff.id == me?.id, onEdit = { editing = staff })
                        if (i < staffList.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                    }
                    if (staffList.isEmpty()) {
                        Text(
                            "No staff yet — you'll be added as the admin on first sign-in.",
                            fontSize = 13.sp,
                            color = c.muted,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
                WideBtn("Add staff member", primary = true, Modifier.fillMaxWidth(), icon = PosIcons.user) { showAdd = true }
                NoticeCard(
                    "Cashiers run the till — selling, products and purchases — but don't see a " +
                        "product's cost or margin or the Bill-wise Profit / Profit & Loss reports, " +
                        "and can't back up, restore or delete the shop's data.",
                )
            }
        }
    }
}

@Composable
private fun StaffRow(
    staff: Staff,
    isSelf: Boolean,
    onEdit: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.raised2).border(1.dp, c.hairline, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PosIcon(PosIcons.user, tint = if (staff.active) c.ink else c.muted, size = 16.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    staff.name + if (isSelf) " (you)" else "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (staff.active) c.ink else c.muted,
                )
                RoleChip(staff)
            }
            Text(
                if (staff.active) "Signs in with their own PIN" else "Deactivated — cannot sign in",
                fontSize = 12.sp,
                color = c.muted,
            )
        }
        SecBtn(null, "Edit", onEdit)
    }
}

@Composable
private fun RoleChip(staff: Staff) {
    val c = PosTheme.colors
    val admin = staff.isAdmin()
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(if (admin) c.amberSoft else c.raised2).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            if (admin) "ADMIN" else "CASHIER",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
            color = if (admin) c.amberPress else c.muted,
        )
    }
}

@Composable
private fun NoticeCard(text: String) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.amberTint).border(1.dp, c.amberSoft, RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosIcon(PosIcons.bell, tint = c.amberPress, size = 16.dp)
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = c.graphite)
    }
}

@Composable
private fun RoleSelector(
    role: StaffRole,
    onRole: (StaffRole) -> Unit,
) {
    val c = PosTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StaffRole.entries.forEach { option ->
            val active = role == option
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) c.ink else c.raised)
                    .border(if (active) 1.5.dp else 1.dp, if (active) c.ink else c.hairline, RoundedCornerShape(10.dp))
                    .clickable { onRole(option) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    if (option == StaffRole.ADMIN) "Admin" else "Cashier",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) c.surface else c.ink,
                )
            }
        }
    }
}

@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, StaffRole) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    val pinValid = pin.length in 4..8 && pin.all { it.isDigit() }
    val valid = name.isNotBlank() && pinValid && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onAdd(name.trim(), pin.trim(), role) }, enabled = valid) { Text("Add staff") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add staff member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("They sign in with this PIN — it identifies them, so it must be unique.", fontSize = 12.sp)
                EditableField("Name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "e.g. Priya")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditableField("PIN (4–8 digits)", pin, { pin = it }, Modifier.weight(1f), mono = true, number = true, placeholder = "····")
                    EditableField("Confirm", confirm, { confirm = it }, Modifier.weight(1f), mono = true, number = true, placeholder = "repeat")
                }
                RoleSelector(role) { role = it }
                if (pin.isNotBlank() && !pinValid) {
                    Text("PIN must be 4–8 digits.", fontSize = 12.sp, color = PosTheme.colors.crimson)
                } else if (confirm.isNotBlank() && pin != confirm) {
                    Text("PINs don't match.", fontSize = 12.sp, color = PosTheme.colors.crimson)
                }
            }
        },
    )
}

@Composable
private fun EditStaffDialog(
    staff: Staff,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, StaffRole, String?) -> Unit,
    onToggleActive: () -> Unit,
) {
    val c = PosTheme.colors
    var name by remember { mutableStateOf(staff.name) }
    var role by remember { mutableStateOf(if (staff.isAdmin()) StaffRole.ADMIN else StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val pinValid = pin.isBlank() || (pin.length in 4..8 && pin.all { it.isDigit() } && pin == confirm)
    val valid = name.isNotBlank() && pinValid
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), role, pin.trim().ifBlank { null }) },
                enabled = valid,
            ) { Text("Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit ${staff.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditableField("Name", name, { name = it }, Modifier.fillMaxWidth())
                RoleSelector(role) { role = it }
                Text("Leave PIN blank to keep the current one.", fontSize = 12.sp, color = c.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditableField("New PIN", pin, { pin = it }, Modifier.weight(1f), mono = true, number = true, placeholder = "····")
                    EditableField("Confirm", confirm, { confirm = it }, Modifier.weight(1f), mono = true, number = true, placeholder = "repeat")
                }
                if (!pinValid) Text("PIN must be 4–8 digits and match.", fontSize = 12.sp, color = c.crimson)
                if (!isSelf) {
                    TextButton(onClick = onToggleActive) {
                        Text(
                            if (staff.active) "Deactivate — block sign-in" else "Reactivate — allow sign-in",
                            color = if (staff.active) c.crimson else c.emerald,
                        )
                    }
                }
            }
        },
    )
}
