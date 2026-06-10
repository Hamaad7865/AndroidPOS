package com.nexapos.retail.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.entity.isAdmin
import com.nexapos.retail.ui.session.currentStaff
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Thread-safe number formatter: builds a fresh [NumberFormat] per call so that
 * concurrent calls from the main thread (composition) and background threads
 * (coroutines, Animatable frame callbacks) never race on shared mutable state.
 * The signature is intentionally kept stable — all call-sites remain unchanged.
 */
fun formatNum(
    value: Double,
    decimals: Int,
): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    fmt.minimumFractionDigits = decimals
    fmt.maximumFractionDigits = decimals
    return fmt.format(value)
}

/** Animated number (count-up) rendered in tabular mono, matching the prototype's CountUp. */
@Composable
fun CountUp(
    value: Double,
    modifier: Modifier = Modifier,
    prefix: String = "",
    decimals: Int = 0,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = PosTheme.colors.ink,
) {
    val anim = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(600)) }
    Text(
        prefix + formatNum(anim.value.toDouble(), decimals),
        modifier = modifier,
        fontFamily = JetBrainsMono,
        fontWeight = fontWeight,
        fontSize = fontSize,
        letterSpacing = (-0.02).em,
        color = color,
    )
}

/** Android tablet status bar (28dp): time, signal, network, battery. */
@Composable
fun StatusBar() {
    val c = PosTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(c.bg)
                .border(width = 0.dp, color = Color.Transparent)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("14:08", fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(3, 5, 8, 11).forEachIndexed { i, h ->
                    Box(
                        Modifier
                            .width(2.5.dp)
                            .height(h.dp)
                            .background(c.ink.copy(alpha = if (i < 3) 0.95f else 0.5f)),
                    )
                }
            }
            Text("LTE", fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = c.ink.copy(alpha = 0.8f))
            Text("87%", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.ink)
            Box(
                Modifier
                    .width(22.dp)
                    .height(10.dp)
                    .border(1.2.dp, c.ink, RoundedCornerShape(2.dp))
                    .padding(1.5.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.8f)
                        .background(c.ink, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

/** Android gesture-navigation pill at the bottom of the screen. */
@Composable
fun GestureBar() {
    val c = PosTheme.colors
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(130.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(c.ink.copy(alpha = 0.65f)),
        )
    }
}

data class NavItem(val id: String, val label: String, val paths: List<String>)

internal val navItems =
    listOf(
        NavItem("home", "Home", PosIcons.home),
        NavItem("pos", "POS", PosIcons.cart),
        NavItem("products", "Products", PosIcons.box),
        NavItem("parties", "Parties", PosIcons.people),
        NavItem("purchase", "Purchase", PosIcons.truck),
        NavItem("money", "Money", PosIcons.wallet),
        NavItem("reports", "Reports", PosIcons.chart),
        NavItem("settings", "Settings", PosIcons.setting),
    )

/** Tablet navigation rail (88dp). */
@Composable
fun NavRail(
    active: String,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier =
            Modifier
                .width(88.dp)
                .fillMaxHeight()
                .background(c.surface)
                .border(width = 1.dp, color = c.hairline)
                // System-bar padding INSIDE the background so the surface paints
                // to the very edges of the screen, while icons still clear any
                // status/gesture insets.
                .systemBarsPadding()
                .padding(top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        // Read SharedPreferences once per composition entry, not every recompose.
        val biz = remember { com.nexapos.retail.data.profile.BusinessProfile.name(context) }
        val bizInitials =
            remember(biz) {
                biz.split(' ', '-', '·')
                    .mapNotNull { token -> token.firstOrNull()?.uppercaseChar()?.toString() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "—" }
            }
        NexaLogo(size = 36.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            "$bizInitials · 01",
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            letterSpacing = 0.14.em,
            color = c.muted,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            // Scrollable so every destination stays reachable on short tablets —
            // the logo stays pinned at top, the cashier slot pinned at the bottom.
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            navItems.forEach { item ->
                val on = active == item.id
                // Memoize the lambda so the Column only recomposes when `item.id`
                // changes (i.e. never — navItems is a top-level val).
                val onClick = remember(item.id) { { onNav(item.id) } }
                Box(contentAlignment = Alignment.CenterStart) {
                    if (on) {
                        Box(
                            Modifier
                                .padding(start = 0.dp)
                                .width(3.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(c.amber),
                        )
                    }
                    Column(
                        modifier =
                            Modifier
                                .width(68.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) c.ink else Color.Transparent)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = item.label
                                }
                                .clickable(onClick = onClick),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PosIcon(
                            item.paths,
                            tint = if (on) c.surface else c.graphite,
                            size = 20.dp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) c.surface else c.graphite,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Bottom slot — the signed-in staff member. Tap to lock the till / sign out.
        SessionSlot(onNav = onNav)
    }
}

/**
 * The signed-in staff badge at the bottom of the nav rail (and the end of the
 * portrait bottom bar). Shows the staff member's initial — amber-ringed for
 * admins — and opens a sign-out dialog. "Sign out" routes through the special
 * "lock" id, which PosApp turns into session.logout() + a clean trip to login.
 */
@Composable
fun SessionSlot(
    onNav: (String) -> Unit,
    compact: Boolean = false,
) {
    val c = PosTheme.colors
    val staff = currentStaff()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        // Warn (but don't block) when a till shift is still open — it survives
        // sign-out and can be closed by the next staff member or an admin.
        val container = (LocalContext.current.applicationContext as PosApplication).container
        val openShift by container.shiftRepository.observeOpenShift().collectAsState(initial = null)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onNav("lock")
                    },
                ) { Text(if (staff != null) "Sign out · lock till" else "Go to sign-in") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text(if (staff != null) "Signed in as ${staff.name}" else "Not signed in") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (staff != null) {
                        Text(
                            if (staff.isAdmin()) "Role: Admin — full access, including profit and cost data." else "Role: Cashier — selling and stock, no profit or cost data.",
                            fontSize = 13.sp,
                        )
                        Text(
                            "Signing out locks the till. The next staff member signs in with their own PIN.",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                        openShift?.let { s ->
                            Text(
                                "⚠ ${s.staffName}'s shift is still open — it stays open after sign-out. " +
                                    "Close it from the Shift screen to balance the drawer.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.amberPress,
                            )
                        }
                    } else {
                        Text("Lock the till and sign in with a staff PIN.", fontSize = 13.sp)
                    }
                }
            },
        )
    }

    val initial = staff?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
    val ring = if (staff?.isAdmin() == true) c.amber else c.hairline
    val description =
        when {
            staff == null -> "Not signed in"
            staff.isAdmin() -> "Signed in as ${staff.name}, admin. Tap to sign out."
            else -> "Signed in as ${staff.name}, cashier. Tap to sign out."
        }
    Box(
        Modifier
            .size(if (compact) 36.dp else 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.raised2)
            .border(if (staff?.isAdmin() == true) 1.5.dp else 1.dp, ring, RoundedCornerShape(12.dp))
            .semantics { contentDescription = description }
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                initial,
                fontFamily = JetBrainsMono,
                fontSize = if (compact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
            )
        } else {
            PosIcon(PosIcons.people, tint = c.muted, size = 18.dp)
        }
    }
}

/** Tablet app bar with business eyebrow, title, optional subtitle and right slot. */
@Composable
fun AppBar(
    title: String,
    subtitle: String? = null,
    right: @Composable (() -> Unit)? = null,
) {
    val c = PosTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            // Read SharedPreferences once and memoize — avoids a disk/prefs
            // read on every recompose triggered by CountUp or other callers.
            val bizName = remember { com.nexapos.retail.data.profile.BusinessProfile.name(context) }
            Text(
                "$bizName · Counter 01",
                fontSize = 11.sp,
                letterSpacing = 0.14.em,
                fontWeight = FontWeight.SemiBold,
                color = c.muted,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    title,
                    fontFamily = HankenGrotesk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.015).em,
                    color = c.ink,
                )
                if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = c.muted)
            }
        }
        right?.invoke()
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}
