package com.nexapos.retail.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.Money

/**
 * Close-shift confirmation: the cashier counts the drawer and types what's
 * actually in it; the dialog previews over/short against the live expectation
 * before anything is committed.
 */
@Composable
fun ShiftCloseDialog(
    expectedCents: Long,
    onDismiss: () -> Unit,
    onConfirm: (countedCents: Long, note: String) -> Unit,
) {
    val c = PosTheme.colors
    var counted by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val countedCents = Money.parseToCents(counted)
    val overShort = countedCents?.minus(expectedCents)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(countedCents ?: 0L, note.trim()) },
                enabled = countedCents != null,
            ) { Text("Close shift") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Count the drawer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Count ALL cash in the drawer (including the float) and enter the total. " +
                        "Expected: ${rsStr(expectedCents)}.",
                    fontSize = 13.sp,
                )
                EditableField(
                    "Counted cash (Rs)",
                    counted,
                    { counted = it },
                    Modifier.fillMaxWidth(),
                    mono = true,
                    number = true,
                    placeholder = Money.toInput(expectedCents),
                )
                overShort?.let { os ->
                    val (label, color) =
                        when {
                            os > 0L -> "Over by ${rsStr(os)}" to c.emerald
                            os < 0L -> "Short by ${rsStr(-os)}" to c.crimson
                            else -> "Balanced — drawer matches exactly." to c.emerald
                        }
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                }
                EditableField(
                    "Note (optional)",
                    note,
                    { note = it },
                    Modifier.fillMaxWidth(),
                    placeholder = "e.g. Rs 100 paid out for cleaning",
                )
                Row {
                    Text(
                        "The report is saved and can be reprinted any time from Shift history.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
            }
        },
    )
}
