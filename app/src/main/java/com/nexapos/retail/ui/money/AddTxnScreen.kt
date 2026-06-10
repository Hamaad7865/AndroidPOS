package com.nexapos.retail.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.FormSection
import com.nexapos.retail.ui.components.GhostBtn
import com.nexapos.retail.ui.components.LabeledField
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.util.Money

/** Shared Record-Expense / Add-Income form (same shape, different labels). */
@Composable
fun AddTxnScreen(
    income: Boolean,
    vm: MoneyViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val title = if (income) "Add Income" else "Record Expense"
    val subtitle = if (income) "Record a new income entry" else "Add a new operating expense"
    val accountLabel = if (income) "Deposit to account" else "Pay from account"

    var category by remember { mutableStateOf(if (income) "Sales" else "Rent") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("Till · Counter 01") }
    var notes by remember { mutableStateOf("") }

    fun save() {
        val cents = Money.parseToCents(amount) ?: 0L
        if (cents <= 0L) return
        val desc = if (notes.isBlank()) description else "$description — $notes".trim(' ', '—')
        vm.addTxn(income, category.trim(), desc, cents, account.trim())
        onBack()
    }

    NavShell(active = "money", onNav = onNav) {
        AppBar(
            title = title,
            subtitle = subtitle,
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostBtn("Cancel", onBack)
                    PrimaryBtn(PosIcons.check, "Save") { save() }
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.width(680.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FormSection(if (income) "Income details" else "Expense details") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField("Category", category, { category = it }, Modifier.weight(1f))
                        LabeledField("Date", "Today", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    EditableField("Description", description, { description = it }, Modifier.fillMaxWidth(), placeholder = "What was this for?")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField("Amount", amount, { amount = it }, Modifier.weight(1f), mono = true, number = true, right = "Rs", placeholder = "0")
                        EditableField(accountLabel, account, { account = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    EditableField("Notes (optional)", notes, { notes = it }, Modifier.fillMaxWidth(), tall = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    WideBtn("Cancel", primary = false, Modifier.width(120.dp), onClick = onBack)
                    WideBtn(
                        if (income) "Save income" else "Save expense",
                        primary = true,
                        Modifier.width(180.dp),
                        icon = PosIcons.check,
                        onClick = { save() },
                    )
                }
            }
        }
    }
}
