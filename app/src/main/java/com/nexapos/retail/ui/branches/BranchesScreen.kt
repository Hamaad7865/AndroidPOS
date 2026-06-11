package com.nexapos.retail.ui.branches

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.branch.BranchDirectory
import com.nexapos.retail.data.branch.BranchIdentity
import com.nexapos.retail.data.branch.BranchRef
import com.nexapos.retail.data.branch.VisibilityMatrix
import com.nexapos.retail.domain.branch.RemoteBranchRepository
import com.nexapos.retail.domain.branch.RemoteBranchState
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.Money
import kotlinx.coroutines.delay

/**
 * Branches — the read-only list of other shops this install may view. Head office
 * sees every branch (plus a consolidated card); a branch sees only the ones the
 * visibility matrix grants it. Each card shows the branch's live summary and how
 * fresh it is. Reached from Settings → Multi-branch (admin, licensed, configured).
 */
@Composable
fun BranchesScreen(
    onNav: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val repo = remember { (context.applicationContext as PosApplication).container.remoteBranches() }
    val myCode = remember { BranchIdentity.code(context) }
    val isHq = remember { BranchIdentity.role(context) == BranchIdentity.Role.HQ }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Branches",
            subtitle = if (isHq) "Head office · viewing all branches" else "Branch $myCode",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (repo == null || !repo.isAvailable()) {
                    BranchCardBox {
                        Eyebrow("Not connected")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Connect this shop to Firebase and sign in (Settings → Multi-branch → Cloud sync) to " +
                                "see the other branches here.",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                    return@Column
                }

                val branches by remember { repo.observeBranches() }.collectAsState(initial = emptyList())
                val matrix by remember { repo.observeVisibility() }.collectAsState(initial = VisibilityMatrix.EMPTY)
                val viewable = BranchDirectory.viewable(myCode, isHq, branches, matrix)

                if (isHq && viewable.isNotEmpty()) {
                    ConsolidatedCard(repo, viewable)
                }
                if (isHq) {
                    WideBtn("Manage visibility", primary = false, Modifier.fillMaxWidth()) { onNav("branch-visibility") }
                }

                if (viewable.isEmpty()) {
                    BranchCardBox {
                        Eyebrow("No branches yet")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isHq) {
                                "No branches have synced yet. Each branch appears here after it connects and syncs once."
                            } else {
                                "Head office hasn't granted this branch visibility of any others yet."
                            },
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                } else {
                    viewable.forEach { ref ->
                        key(ref.code) { BranchSummaryCard(repo, ref) { onOpenBranch(ref.code) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsolidatedCard(
    repo: RemoteBranchRepository,
    viewable: List<BranchRef>,
) {
    val c = PosTheme.colors
    val states =
        viewable.map { ref ->
            key(ref.code) {
                remember(ref.code) { repo.observeState(ref.code) }.collectAsState(RemoteBranchState(null, null)).value
            }
        }
    val totals = BranchDirectory.consolidate(states.mapNotNull { it.summary })
    BranchCardBox {
        Eyebrow("All branches · today")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Sales", fontSize = 11.sp, color = c.muted)
                Text(Money.format(totals.salesTodayCents), fontFamily = JetBrainsMono, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${totals.ticketsToday} tickets · ${totals.branchCount} branches", fontSize = 11.sp, color = c.muted)
                Text("Stock ${Money.format(totals.stockValueCents)}", fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.ink)
            }
        }
    }
}

@Composable
private fun BranchSummaryCard(
    repo: RemoteBranchRepository,
    ref: BranchRef,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    val state by remember(ref.code) { repo.observeState(ref.code) }.collectAsState(initial = RemoteBranchState(null, null))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CodeBadge(ref.code, ref.isHq)
            Text(ref.name.ifBlank { ref.code }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
            SyncLabel(state.lastSyncAt)
        }
        val s = state.summary
        Spacer(Modifier.height(10.dp))
        if (s == null) {
            Text("No data yet", fontSize = 12.sp, color = c.muted)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Today", fontSize = 11.sp, color = c.muted)
                    Text(Money.format(s.salesTodayCents), fontFamily = JetBrainsMono, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.ticketsToday} tickets · ${s.itemCount} SKUs", fontSize = 11.sp, color = c.muted)
                    if (s.lowStockCount > 0) {
                        Text("${s.lowStockCount} low stock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.low)
                    }
                    s.openShiftStaff?.let { Text("Open · $it", fontSize = 11.sp, color = c.emerald) }
                }
            }
        }
    }
}

@Composable
internal fun CodeBadge(
    code: String,
    isHq: Boolean,
) {
    val c = PosTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (isHq) c.amber else c.raised2).border(1.dp, c.hairline, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(code, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isHq) Color.White else c.ink)
    }
}

@Composable
internal fun SyncLabel(lastSyncAt: Long?) {
    val c = PosTheme.colors
    if (lastSyncAt == null) {
        Text("Never synced", fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = c.muted)
        return
    }
    // Tick once a minute so the freshness label ages while the screen stays open.
    val nowMs by produceState(initialValue = System.currentTimeMillis(), lastSyncAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000)
        }
    }
    // coerceAtLeast(0): a peer device's clock running ahead must not read as "future".
    val mins = ((nowMs - lastSyncAt) / 60_000).coerceAtLeast(0)
    val label =
        when {
            mins < 1 -> "just now"
            mins < 60 -> "$mins min ago"
            mins < 1_440 -> "${mins / 60} h ago"
            else -> "${mins / 1_440} d ago"
        }
    val color =
        when {
            mins > 1_440 -> c.crimson
            mins > 60 -> c.low
            else -> c.muted
        }
    Text("Synced $label", fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = color)
}

@Composable
internal fun BranchCardBox(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(18.dp),
        content = content,
    )
}
