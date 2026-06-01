package com.nexapos.retail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.theme.PosTheme

/** Placeholder for nav destinations not yet built, so the nav rail works end-to-end. */
@Composable
fun ComingSoonScreen(
    title: String,
    navId: String,
    icon: List<String>,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    NavShell(active = navId, onNav = onNav) {
        AppBar(title = title)
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.raised2)
                        .border(1.dp, c.hairline, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PosIcon(icon, tint = c.amber, size = 32.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text("$title — coming soon", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text("This module is being built next.", fontSize = 14.sp, color = c.muted)
            }
        }
    }
}
