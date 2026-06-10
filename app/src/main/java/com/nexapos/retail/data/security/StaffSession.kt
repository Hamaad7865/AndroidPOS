package com.nexapos.retail.data.security

import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.isAdmin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Who is signed in at this till right now. In-memory only — every cold start
 * lands on the login screen, so the session never outlives the process.
 * Held on the AppContainer; UI observes [current] to react to role changes.
 */
class StaffSession {
    private val mutableCurrent = MutableStateFlow<Staff?>(null)
    val current: StateFlow<Staff?> = mutableCurrent.asStateFlow()

    /** Null session (e.g. mid-setup) gets the most restricted view. */
    val isAdmin: Boolean get() = mutableCurrent.value?.isAdmin() == true

    fun login(staff: Staff) {
        mutableCurrent.value = staff
    }

    fun logout() {
        mutableCurrent.value = null
    }
}
