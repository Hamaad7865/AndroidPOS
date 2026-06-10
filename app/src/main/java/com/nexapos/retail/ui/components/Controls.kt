package com.nexapos.retail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier = modifier, fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

@Composable
fun SecBtn(
    icon: List<String>?,
    label: String,
    onClick: () -> Unit = {},
) {
    val c = PosTheme.colors
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) PosIcon(icon, tint = c.ink, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
fun PrimaryBtn(
    icon: List<String>?,
    label: String,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(PosTheme.colors.amber).clickable { onClick() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) PosIcon(icon, tint = Color.White, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun Chip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier.height(34.dp).clip(CircleShape).background(if (active) c.ink else c.raised).border(1.dp, if (active) c.ink else c.hairline, CircleShape).clickable { onClick() }.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) c.surface else c.ink)
    }
}

@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    Row(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosIcon(PosIcons.search, tint = c.ink, size = 16.dp)
        Box(Modifier.weight(1f)) {
            BasicTextField(value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = HankenGrotesk, fontSize = 14.sp, color = c.ink), cursorBrush = SolidColor(c.amber))
            if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = c.muted)
        }
    }
}

/** Segmented control used by Parties / Money. */
@Composable
fun TabBar(
    tabs: List<Pair<String, String>>,
    active: String,
    onSelect: (String) -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { (id, label) ->
            val on = active == id
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(8.dp)).background(if (on) c.ink else Color.Transparent).clickable { onSelect(id) }.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) c.surface else c.ink)
            }
        }
    }
}

enum class BadgeKind { PAID, AMBER, DUE, GHOST, LOW }

@Composable
fun StatusBadge(
    text: String,
    kind: BadgeKind,
) {
    val c = PosTheme.colors
    val (bg, fg) =
        when (kind) {
            BadgeKind.PAID -> c.emeraldSoft to c.emerald
            BadgeKind.AMBER -> c.amberSoft to c.amberPress
            BadgeKind.DUE -> c.crimsonSoft to c.crimson
            BadgeKind.GHOST -> c.raised2 to c.graphite
            BadgeKind.LOW -> c.lowSoft to c.low
        }
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

fun rsStr(n: Int) = "Rs " + formatNum(n.toDouble(), 0)

/** Money display from exact cents, with 2 decimals (750 -> "Rs 7.50"). */
fun rsStr(cents: Long) = com.nexapos.retail.util.Money.format(cents)
