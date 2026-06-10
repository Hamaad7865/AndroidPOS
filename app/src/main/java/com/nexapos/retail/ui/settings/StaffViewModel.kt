package com.nexapos.retail.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.data.security.StaffSession
import com.nexapos.retail.domain.repository.StaffRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Staff & roles management. Mutations surface their outcome via [message] —
 * the repository throws on rule violations (duplicate PIN, last admin).
 */
class StaffViewModel(
    private val staffRepository: StaffRepository,
    private val session: StaffSession,
) : ViewModel() {
    val staff: StateFlow<List<Staff>> =
        staffRepository.observeStaff()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** One-line feedback for the last action; null = nothing to show. */
    var message: String? by mutableStateOf(null)
        private set

    fun clearMessage() {
        message = null
    }

    fun addStaff(
        name: String,
        pin: String,
        role: StaffRole,
    ) = mutate("${role.name.lowercase().replaceFirstChar { it.uppercase() }} \"$name\" added.") {
        staffRepository.addStaff(name, pin, role)
    }

    fun rename(
        staff: Staff,
        name: String,
    ) = mutate("Renamed to \"$name\".") { staffRepository.rename(staff.id, name) }

    fun setRole(
        staff: Staff,
        role: StaffRole,
    ) = mutate("${staff.name} is now a ${role.name.lowercase()}.") { staffRepository.setRole(staff.id, role) }

    fun resetPin(
        staff: Staff,
        pin: String,
    ) = mutate("PIN updated for ${staff.name}.") { staffRepository.setPin(staff.id, pin) }

    fun setActive(
        staff: Staff,
        active: Boolean,
    ) = mutate(if (active) "${staff.name} can sign in again." else "${staff.name} deactivated.") {
        staffRepository.setActive(staff.id, active)
    }

    /**
     * Runs a repository mutation off the main thread (its PBKDF2 PIN hashing is
     * CPU-heavy), then refreshes the signed-in user's session so a self-edit
     * (e.g. demoting yourself) takes effect immediately instead of after the
     * next sign-in. Rule violations throw with a readable message.
     */
    private fun mutate(
        successMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            message =
                withContext(Dispatchers.Default) {
                    try {
                        action()
                        refreshCurrentSession()
                        successMessage
                    } catch (e: IllegalArgumentException) {
                        e.message
                    } catch (e: IllegalStateException) {
                        e.message
                    }
                }
        }
    }

    /**
     * Keeps [StaffSession] in step with the DB for the signed-in user. After an
     * edit to their own row, role gating must not run on a stale snapshot — so we
     * reload them (or sign them out if they were just deactivated/removed).
     */
    private suspend fun refreshCurrentSession() {
        val me = session.current.value ?: return
        val fresh = staffRepository.activeStaff().firstOrNull { it.id == me.id }
        if (fresh == null) session.logout() else session.login(fresh)
    }
}
