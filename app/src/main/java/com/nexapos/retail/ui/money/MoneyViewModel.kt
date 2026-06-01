package com.nexapos.retail.ui.money

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.domain.repository.MoneyRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * One line in the cash ledger. Money-in lives in [inRupees] (sales takings and
 * manual income); money-out in [outRupees] (manual expenses). [balanceRupees] is
 * the running cash balance across the shown rows, computed oldest → newest.
 */
data class LedgerLine(
    val createdAt: Long,
    val ref: String,
    /** "sale" | "income" | "expense" — drives the row badge + colour. */
    val type: String,
    val description: String,
    val account: String,
    val inRupees: Int,
    val outRupees: Int,
    val balanceRupees: Int,
)

/** Per-account cash rollup from manual income/expense entries. */
data class AccountSummary(
    val name: String,
    val inRupees: Int,
    val outRupees: Int,
) {
    val netRupees: Int get() = inRupees - outRupees
}

/**
 * Backs the Money hub, income, expense and ledger screens. Income/expense rows
 * are manual cash-book entries; sales takings are pulled from [SalesRepository]
 * so the headline figures and ledger reflect real money movement.
 */
class MoneyViewModel(
    private val moneyRepository: MoneyRepository,
    salesRepository: SalesRepository,
) : ViewModel() {
    var incomes by mutableStateOf<List<MoneyTxn>>(emptyList())
        private set

    var expenses by mutableStateOf<List<MoneyTxn>>(emptyList())
        private set

    /** Recent completed sales — counted as money-in in the ledger and income tab. */
    var recentSales by mutableStateOf<List<Sale>>(emptyList())
        private set

    var manualIncomeMonthCents by mutableStateOf(0L)
        private set

    var expenseMonthCents by mutableStateOf(0L)
        private set

    var salesMonthCents by mutableStateOf(0L)
        private set

    private val monthStart = startOfMonth()

    init {
        viewModelScope.launch { moneyRepository.observeIncome().collect { incomes = it } }
        viewModelScope.launch { moneyRepository.observeExpenses().collect { expenses = it } }
        viewModelScope.launch { salesRepository.observeRecent().collect { recentSales = it } }
        viewModelScope.launch {
            moneyRepository.observeSumSince(MoneyTxn.TYPE_INCOME, monthStart).collect { manualIncomeMonthCents = it }
        }
        viewModelScope.launch {
            moneyRepository.observeSumSince(MoneyTxn.TYPE_EXPENSE, monthStart).collect { expenseMonthCents = it }
        }
        viewModelScope.launch { salesRepository.observeTotalSince(monthStart).collect { salesMonthCents = it } }
    }

    /** Income this month = sales takings + manual income entries, in whole rupees. */
    val incomeMonth: Int get() = ((salesMonthCents + manualIncomeMonthCents) / CENTS_PER_RUPEE).toInt()
    val expenseMonth: Int get() = (expenseMonthCents / CENTS_PER_RUPEE).toInt()
    val netMonth: Int get() = incomeMonth - expenseMonth

    /** Just the sales slice of income this month (whole rupees). */
    val salesMonth: Int get() = (salesMonthCents / CENTS_PER_RUPEE).toInt()

    /**
     * Recent cash ledger: completed sales + manual income (money in) and manual
     * expenses (money out), newest first, with a running balance across the rows.
     */
    val ledger: List<LedgerLine>
        get() {
            val lines = ArrayList<LedgerLine>(recentSales.size + incomes.size + expenses.size)
            recentSales.forEach { s ->
                lines +=
                    LedgerLine(
                        createdAt = s.createdAt,
                        ref = s.receiptNo,
                        type = "sale",
                        description = "Sale · ${s.customerName.ifBlank { "Walk-in" }}",
                        account = s.paymentMethod.lowercase().replaceFirstChar { it.uppercase() },
                        inRupees = (s.totalCents / CENTS_PER_RUPEE).toInt(),
                        outRupees = 0,
                        balanceRupees = 0,
                    )
            }
            incomes.forEach { t ->
                lines +=
                    LedgerLine(
                        createdAt = t.createdAt,
                        ref = t.code.ifEmpty { "INC-${t.id}" },
                        type = "income",
                        description = t.description.ifBlank { t.category }.ifBlank { "Income" },
                        account = t.account.ifBlank { "Unassigned" },
                        inRupees = (t.amountCents / CENTS_PER_RUPEE).toInt(),
                        outRupees = 0,
                        balanceRupees = 0,
                    )
            }
            expenses.forEach { t ->
                lines +=
                    LedgerLine(
                        createdAt = t.createdAt,
                        ref = t.code.ifEmpty { "EXP-${t.id}" },
                        type = "expense",
                        description = t.description.ifBlank { t.category }.ifBlank { "Expense" },
                        account = t.account.ifBlank { "Unassigned" },
                        inRupees = 0,
                        outRupees = (t.amountCents / CENTS_PER_RUPEE).toInt(),
                        balanceRupees = 0,
                    )
            }
            var balance = 0
            val ascendingWithBalance =
                lines.sortedBy { it.createdAt }.map { line ->
                    balance += line.inRupees - line.outRupees
                    line.copy(balanceRupees = balance)
                }
            return ascendingWithBalance.asReversed()
        }

    /** Per-account rollup from manual entries (walk-in sales aren't tied to an account). */
    val accounts: List<AccountSummary>
        get() {
            val buckets = LinkedHashMap<String, IntArray>() // name -> [in, out]

            fun bucket(name: String) = buckets.getOrPut(name.ifBlank { "Unassigned" }) { intArrayOf(0, 0) }
            incomes.forEach { bucket(it.account)[0] += (it.amountCents / CENTS_PER_RUPEE).toInt() }
            expenses.forEach { bucket(it.account)[1] += (it.amountCents / CENTS_PER_RUPEE).toInt() }
            return buckets
                .map { (name, io) -> AccountSummary(name, io[0], io[1]) }
                .sortedByDescending { it.inRupees + it.outRupees }
        }

    fun addTxn(
        income: Boolean,
        category: String,
        description: String,
        amountRupees: Int,
        account: String,
    ) {
        if (amountRupees <= 0) return
        viewModelScope.launch {
            moneyRepository.add(
                MoneyTxn(
                    type = if (income) MoneyTxn.TYPE_INCOME else MoneyTxn.TYPE_EXPENSE,
                    category = category,
                    description = description.trim(),
                    amountCents = amountRupees * CENTS_PER_RUPEE,
                    account = account,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
