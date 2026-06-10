package com.nexapos.retail.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.branch.MultiBranch
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.theme.PosTheme
import java.time.LocalDate

/**
 * Settings → Multi-branch. Entry point for the paid multi-branch add-on: enter
 * the offline licence key to unlock it, or view / remove an active licence.
 * Branch setup and cross-branch viewing build on top of this in later updates.
 */
@Composable
fun MultiBranchSettingsScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var license by remember { mutableStateOf(MultiBranch.license(context)) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Multi-branch",
            subtitle = "Link several shops · paid add-on",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card {
                    Eyebrow("What it is")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Connect branches and a head office. Each branch keeps selling fully offline; when " +
                            "online they share read-only reports, stock and sales, so any allowed branch — and " +
                            "head office — can check the others. This is a paid add-on, unlocked with a licence key.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                val active = license
                if (active == null) {
                    Card {
                        Eyebrow("Activate")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enter the licence key you were given. It is tied to your business name and verified " +
                                "on this device — no internet needed.",
                            fontSize = 11.sp,
                            color = c.muted,
                        )
                        Spacer(Modifier.height(10.dp))
                        EditableField(
                            "Licence key",
                            code,
                            {
                                code = it
                                error = null
                            },
                            Modifier.fillMaxWidth(),
                            mono = true,
                            placeholder = "NXB-…",
                        )
                        error?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            Text(msg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.crimson)
                        }
                        Spacer(Modifier.height(12.dp))
                        WideBtn("Activate", primary = true, Modifier.fillMaxWidth(), icon = PosIcons.check) {
                            val result = MultiBranch.activate(context, code)
                            if (result == null) {
                                error =
                                    "That key isn't valid for “${BusinessProfile.name(context)}”. Make sure it was " +
                                    "issued for this exact business name."
                            } else {
                                license = result
                                code = ""
                                error = null
                            }
                        }
                    }
                } else {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("●", fontSize = 14.sp, color = c.emerald)
                            Text("Active", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.emerald)
                        }
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Licensed to", active.business)
                        InfoRow("Branches allowed", active.maxBranches.toString())
                        InfoRow(
                            "Expires",
                            if (active.expiryEpochDay == 0L) "Never" else LocalDate.ofEpochDay(active.expiryEpochDay).toString(),
                        )
                        Spacer(Modifier.height(12.dp))
                        WideBtn("Remove licence", primary = false, Modifier.fillMaxWidth()) {
                            MultiBranch.deactivate(context)
                            license = null
                        }
                    }
                    Card {
                        Eyebrow("Next")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Branch setup (head office vs branch, and which branches can see each other) and " +
                                "cross-branch viewing arrive in upcoming updates.",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = c.muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        content = content,
    )
}
