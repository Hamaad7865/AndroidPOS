package com.nexapos.retail.ui.shift

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.data.entity.isAdmin
import com.nexapos.retail.data.security.StaffSession
import com.nexapos.retail.domain.ShiftSummary
import com.nexapos.retail.domain.StaffPolicy
import com.nexapos.retail.domain.repository.ShiftRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Drives the shift screens: open with a float, live summary while open, close
 * with a counted drawer, history + reprint. Instantiated once at PosApp level
 * (like SellingViewModel) so [promptDismissed] survives navigation.
 */
class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val session: StaffSession,
) : ViewModel() {
    val openShift: StateFlow<Shift?> =
        shiftRepository.observeOpenShift()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Live numbers for the OPEN shift; recomputed as stamped rows land. */
    var liveSummary by mutableStateOf<ShiftSummary?>(null)
        private set

    /** Frozen summary shown right after closing (drives the report view). */
    var justClosed by mutableStateOf<ShiftSummary?>(null)
        private set

    /** "Open a shift?" prompt after login — dismissible once per sign-in. */
    var promptDismissed by mutableStateOf(false)

    var error by mutableStateOf<String?>(null)
        private set

    var history by mutableStateOf<List<Shift>>(emptyList())
        private set

    /** Summary of a past shift opened from history (reprint). */
    var historyDetail by mutableStateOf<ShiftSummary?>(null)
        private set

    private var summaryJob: Job? = null
    private var historyJob: Job? = null

    init {
        viewModelScope.launch {
            openShift.collect { shift ->
                summaryJob?.cancel()
                liveSummary = null
                if (shift != null) {
                    summaryJob =
                        viewModelScope.launch {
                            shiftRepository.observeSummary(shift.id).collect { liveSummary = it }
                        }
                }
            }
        }
    }

    fun clearError() {
        error = null
    }

    /** Opens a shift for the signed-in staff with [floatCents] in the drawer. */
    fun open(floatCents: Long) {
        val staff = session.current.value
        if (staff == null) {
            error = "Sign in before opening a shift."
            return
        }
        viewModelScope.launch {
            error =
                try {
                    shiftRepository.openShift(
                        staffId = staff.id,
                        staffName = staff.name,
                        openingFloatCents = floatCents,
                    )
                    justClosed = null
                    null
                } catch (e: IllegalStateException) {
                    e.message
                }
        }
    }

    /** Closes the open shift with the counted drawer cash. */
    fun close(
        countedCents: Long,
        note: String,
    ) {
        val shift = openShift.value ?: return
        viewModelScope.launch {
            error =
                try {
                    shiftRepository.closeShift(
                        shiftId = shift.id,
                        declaredCashCents = countedCents,
                        note = note,
                    )
                    justClosed = shiftRepository.summary(shift.id)
                    null
                } catch (e: IllegalStateException) {
                    e.message
                }
        }
    }

    fun dismissJustClosed() {
        justClosed = null
    }

    /** Loads shifts the signed-in staff may see: admins all, cashiers their own. */
    fun loadHistory() {
        val staff = session.current.value
        historyJob?.cancel()
        if (staff == null) {
            history = emptyList()
            return
        }
        historyJob =
            viewModelScope.launch {
                val flow =
                    if (staff.isAdmin()) {
                        shiftRepository.observeHistory()
                    } else {
                        shiftRepository.observeHistoryFor(staff.id)
                    }
                flow.collect { shifts ->
                    history =
                        shifts.filter {
                            StaffPolicy.canSeeShift(staff.isAdmin(), staff.id, it.staffId)
                        }
                }
            }
    }

    fun loadDetail(shiftId: Long) {
        viewModelScope.launch {
            historyDetail =
                try {
                    shiftRepository.summary(shiftId)
                } catch (e: IllegalStateException) {
                    error = e.message
                    null
                }
        }
    }

    fun clearDetail() {
        historyDetail = null
    }

    /** Called from the sign-out path: next staff member gets a fresh prompt. */
    fun onSignOut() {
        promptDismissed = false
        justClosed = null
        historyDetail = null
        error = null
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
