package com.nexapos.retail.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.isAdmin

/** The signed-in staff member, observed so the UI recomposes on login/logout. */
@Composable
fun currentStaff(): Staff? {
    val app = LocalContext.current.applicationContext as PosApplication
    val staff by app.container.session.current.collectAsState()
    return staff
}

/** True only for a signed-in admin — a null session gets the cashier view. */
@Composable
fun rememberIsAdmin(): Boolean = currentStaff()?.isAdmin() == true
