package com.nexapos.retail.ui.branches

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.branch.VisibilityMatrix
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.launch

/**
 * Head-office-only: grant each branch the right to view specific other branches.
 * Head office always sees everyone; this controls branch-to-branch visibility.
 * Each toggle saves the whole matrix immediately.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisibilityEditorScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val repo = remember { (context.applicationContext as PosApplication).container.remoteBranches() }
    val scope = rememberCoroutineScope()

    NavShell(active = "settings", onNav = onNav) {
        AppBar(title = "Branch visibility", subtitle = "Who can view whom", right = { SecBtn(null, "Back", onBack) })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (repo == null) {
                    BranchCardBox { Text("Not connected.", fontSize = 12.sp, color = c.muted) }
                    return@Column
                }
                val branches by remember { repo.observeBranches() }.collectAsState(initial = emptyList())
                val saved by remember { repo.observeVisibility() }.collectAsState(initial = VisibilityMatrix.EMPTY)
                // Adopt the remote matrix until the first local edit, then local edits win — so a
                // toggle's own Firestore write echoing back can't reset/revert in-flight changes.
                var working by remember { mutableStateOf<Map<String, List<String>>?>(null) }
                val current = working ?: saved.canView
                val viewers = branches.filter { !it.isHq }

                BranchCardBox {
                    Eyebrow("How it works")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Head office always sees every branch. Tap a chip to let that branch view another branch's " +
                            "reports, stock and sales. Changes save instantly.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                if (viewers.isEmpty()) {
                    BranchCardBox { Text("No branches have synced yet.", fontSize = 12.sp, color = c.muted) }
                }

                viewers.forEach { viewer ->
                    key(viewer.code) {
                        BranchCardBox {
                            Eyebrow("${viewer.name.ifBlank { viewer.code }} can view")
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                branches.filter { it.code != viewer.code }.forEach { target ->
                                    val granted = current[viewer.code].orEmpty().contains(target.code)
                                    ToggleChip(target.code, granted) {
                                        val list = current[viewer.code].orEmpty()
                                        val next = if (granted) list - target.code else list + target.code
                                        val updated = current + (viewer.code to next)
                                        working = updated
                                        scope.launch { repo.saveVisibility(VisibilityMatrix(updated)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    on: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) c.emerald else c.raised2).border(1.dp, if (on) c.emerald else c.hairline, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else c.ink)
    }
}
